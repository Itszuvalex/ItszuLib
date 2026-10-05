package com.itszuvalex.itszulib.verify

import com.itszuvalex.itszulib.api.adapters.IFluidStack
import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.core.BlockEntityCore
import com.itszuvalex.itszulib.core.BreakBehavior
import com.itszuvalex.itszulib.core.IBreakContents
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.core.frag.FragDropInventory
import com.itszuvalex.itszulib.core.frag.FragEnergyStorage
import com.itszuvalex.itszulib.core.frag.FragFluidStorage
import com.itszuvalex.itszulib.core.frag.FragItemStorage
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import java.util.Collections
import java.util.IdentityHashMap
import java.util.TreeMap

/**
 * Gives block entities contents worth saving, dropping or carrying, and measures what they hold, for the checks in
 * this package ([BlockEntityRoundTrip], [BreakChecks], [MenuChecks]). Covers what ItszuLib's storage fragments hold
 * (items, fluids, energy); a mod with contents of its own adds a [Probe] ([register]).
 */
object BlockEntityContents {
    /**
     * What a block entity holds, as counts by name: items (`item#componentsHash`), fluids and any other thing a probe
     * measures. Energy is rounded to whole units.
     */
    data class Tally(val counts: Map<String, Long>) {
        operator fun plus(other: Tally): Tally = Tally(TreeMap(counts).also { m -> other.counts.forEach { (k, v) -> m.merge(k, v, Long::plus) } })

        fun isEmpty(): Boolean = counts.values.all { it == 0L }

        /** What [other] has that this has not, or has less of; empty when [other] is covered. */
        fun missingFrom(other: Tally): Map<String, Long> =
            counts.mapNotNull { (k, v) -> (v - (other.counts[k] ?: 0L)).takeIf { it > 0L }?.let { k to it } }.toMap()

        override fun toString(): String = counts.entries.filter { it.value != 0L }.joinToString(", ") { "${it.value} ${it.key}" }.ifEmpty { "nothing" }

        companion object {
            val EMPTY = Tally(emptyMap())
        }
    }

    /**
     * Contents of a kind ItszuLib does not know. [fill] and [tally] cover the fragments whose [IBreakContents.breakBehavior]
     * passes the filter given to them.
     */
    interface Probe {
        /** Puts something distinctive in what [be] holds of this kind. @return Whether it held any. */
        fun fill(be: BlockEntityCore, behaviors: Set<BreakBehavior>): Boolean

        /** What [be] holds of this kind. */
        fun tally(be: BlockEntityCore, behaviors: Set<BreakBehavior>): Tally
    }

    private val probes = ArrayList<Probe>()

    /** Adds a probe for contents of a kind ItszuLib does not know (nanites, say). */
    @JvmStatic
    fun register(probe: Probe) {
        probes += probe
    }

    private val FILL_ITEMS = listOf(Items.DIAMOND, Items.GOLD_INGOT, Items.IRON_INGOT, Items.EMERALD, Items.COAL, Items.REDSTONE)

    private fun owned(be: BlockEntityCore, behaviors: Set<BreakBehavior>): List<IBreakContents> =
        be.contentFragments().filter { it.breakBehavior in behaviors }

    /** Every behaviour, for measuring everything a block entity holds. */
    @JvmField
    val ALL: Set<BreakBehavior> = BreakBehavior.entries.toSet()

    /**
     * Puts distinctive contents in every storage [be] owns whose break behaviour is in [behaviors] (all by default):
     * different items, counts and fluids per slot, and half a battery. Storages are filled directly, so a slot's
     * filter does not stop it. @return Whether it held anything fillable.
     */
    @JvmStatic
    @JvmOverloads
    fun fill(be: BlockEntityCore, behaviors: Set<BreakBehavior> = ALL): Boolean {
        var filled = false
        val seen = Collections.newSetFromMap(IdentityHashMap<IItemStorage, Boolean>())
        for (frag in owned(be, behaviors)) {
            when (frag) {
                is FragItemStorage, is FragDropInventory -> itemStorage(frag)?.takeIf { seen.add(it) }?.let { s ->
                    for (i in 0 until s.size()) {
                        s.setSlot(i, IItemStack.of(ItemStack(FILL_ITEMS[i % FILL_ITEMS.size], i + 1)))
                        filled = true
                    }
                }
                is FragFluidStorage -> if (frag.persist) {
                    val s = frag.storage
                    for (i in 0 until s.size()) {
                        val fluid = if (i % 2 == 0) Fluids.WATER else Fluids.LAVA
                        s.set(i, IFluidStack.of(FluidStack(fluid, minOf(s.capacity(i), 250 * (i + 1)))))
                        filled = true
                    }
                }
                is FragEnergyStorage -> if (frag.persist) {
                    frag.storage.setStorage(frag.storage.maxStorage() / 2)
                    filled = true
                }
            }
        }
        for (probe in probes) filled = probe.fill(be, behaviors) || filled
        return filled
    }

    /**
     * What [be] holds in the storages it owns whose break behaviour is in [behaviors] (all by default).
     */
    @JvmStatic
    @JvmOverloads
    fun tally(be: BlockEntityCore, behaviors: Set<BreakBehavior> = ALL): Tally {
        val counts = TreeMap<String, Long>()
        fun add(key: String, n: Long) {
            if (n != 0L) counts.merge(key, n, Long::plus)
        }
        val seen = Collections.newSetFromMap(IdentityHashMap<IItemStorage, Boolean>())
        for (frag in owned(be, behaviors)) {
            when (frag) {
                is FragItemStorage, is FragDropInventory -> itemStorage(frag)?.takeIf { seen.add(it) }?.let { s ->
                    for (i in 0 until s.size()) {
                        val stack = s.get(i).toMinecraft()
                        if (!stack.isEmpty) add(itemKey(stack), stack.count.toLong())
                    }
                }
                is FragFluidStorage -> if (frag.persist) for (i in 0 until frag.storage.size()) {
                    val stack = frag.storage.get(i)
                    if (!stack.isEmpty()) add("fluid:${stack.fluid()}", stack.amount().toLong())
                }
                is FragEnergyStorage -> if (frag.persist) add("energy", Math.round(frag.storage.storage()))
            }
        }
        var tally = Tally(counts)
        for (probe in probes) tally += probe.tally(be, behaviors)
        return tally
    }

    /** The item storage a fragment owns and holds contents in, if it is one. */
    private fun itemStorage(frag: IBreakContents): IItemStorage? = when (frag) {
        is FragItemStorage -> frag.storage.takeIf { frag.persist }
        is FragDropInventory -> frag.storage
        else -> null
    }

    /** The key [tally] counts an item stack under. */
    @JvmStatic
    fun itemKey(stack: ItemStack): String = "${BuiltInRegistries.ITEM.getKey(stack.item)}#${ItemStack.hashItemAndComponents(stack)}"
}
