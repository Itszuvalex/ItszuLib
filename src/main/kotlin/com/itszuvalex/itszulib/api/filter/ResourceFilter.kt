package com.itszuvalex.itszulib.api.filter

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.FluidUtil
import java.util.Objects

/** Whether a [ResourceFilter]'s entries are the only things let through, or the things kept out. */
enum class FilterMode {
    ALLOW,
    DENY,
    ;

    fun toggled(): FilterMode = if (this == ALLOW) DENY else ALLOW
}

/**
 * What a [ResourceFilter] filters: how to tell its things apart, make an entry of one, and read one from a held item
 * (for click-to-set screens). [ItemStack]s and [FluidStack]s come with ItszuLib ([FilterKinds]); a mod adds its own
 * kinds for anything else it moves.
 */
interface FilterKind<T : Any> {
    /** An unused entry. */
    val empty: T

    val codec: Codec<T>

    fun isEmpty(t: T): Boolean

    /** Whether [a] and [b] are the same thing, ignoring amounts; with [components], their data must match too. */
    fun same(a: T, b: T, components: Boolean): Boolean

    /** [t] as an entry (one of it). */
    fun entryOf(t: T): T

    /**
     * The entry a click with [held] sets: [empty] for an empty hand (it clears), the thing [held] is or holds, or null
     * if [held] means nothing to this kind.
     */
    fun fromHeld(held: ItemStack): T?

    /** Value equality of entries (stacks do not compare by value). */
    fun equal(a: T, b: T): Boolean

    fun hash(t: T): Int
}

/**
 * An allowlist or denylist of up to [size] entries of a [kind]: with no entries everything passes; otherwise, in
 * [FilterMode.ALLOW], only things the same as an entry pass, and in [FilterMode.DENY], everything else does. Entries
 * match by thing alone, or also by data components if [matchComponents]. Immutable: changes return a new filter.
 */
class ResourceFilter<T : Any> @JvmOverloads constructor(
    val kind: FilterKind<T>,
    val size: Int,
    entries: List<T> = emptyList(),
    val mode: FilterMode = FilterMode.ALLOW,
    val matchComponents: Boolean = true,
) {
    /** Always [size] entries; unused ones are [FilterKind.empty]. */
    val entries: List<T> = List(size) { i -> entries.getOrNull(i)?.takeUnless(kind::isEmpty)?.let(kind::entryOf) ?: kind.empty }

    /** Whether no entry is set (everything passes). */
    val isEmpty: Boolean get() = entries.all(kind::isEmpty)

    /** The entries that are set. */
    fun listed(): List<T> = entries.filterNot(kind::isEmpty)

    /** Whether [t] is the same as an entry. */
    fun lists(t: T): Boolean = entries.any { !kind.isEmpty(it) && kind.same(it, t, matchComponents) }

    /** Whether [t] passes. */
    fun test(t: T): Boolean = isEmpty || lists(t) == (mode == FilterMode.ALLOW)

    /**
     * The things that alone can pass, when the filter names them ([FilterMode.ALLOW] with entries), or null when
     * anything not listed could pass. Lets a lookup ask for exactly these (e.g. through an index) instead of scanning.
     */
    fun only(): List<T>? = if (mode == FilterMode.ALLOW && !isEmpty) listed() else null

    fun withEntry(index: Int, entry: T): ResourceFilter<T> {
        require(index in 0 until size) { "Filter entry $index out of 0 until $size" }
        return ResourceFilter(kind, size, entries.toMutableList().also { it[index] = entry }, mode, matchComponents)
    }

    fun withMode(mode: FilterMode): ResourceFilter<T> = ResourceFilter(kind, size, entries, mode, matchComponents)

    fun withMatchComponents(match: Boolean): ResourceFilter<T> = ResourceFilter(kind, size, entries, mode, match)

    /** The same filter over another [kind], its entries converted by [convert] (e.g. item stacks to ItszuLib's). */
    fun <U : Any> map(kind: FilterKind<U>, convert: (T) -> U): ResourceFilter<U> =
        ResourceFilter(kind, size, entries.map { if (this.kind.isEmpty(it)) kind.empty else convert(it) }, mode, matchComponents)

    /**
     * The filter after [action] ([FilterActions]) with [held] in hand: a cell set from [held] (or cleared by an empty
     * hand), the mode or the component matching toggled. Null if nothing changes or the action makes no sense (an
     * unknown action or cell, or [held] means nothing to the kind).
     */
    fun apply(action: Int, held: ItemStack): ResourceFilter<T>? = when (FilterActions.op(action)) {
        FilterActions.SET -> FilterActions.cell(action).takeIf { it in 0 until size }?.let { cell -> kind.fromHeld(held)?.let { withEntry(cell, it) } }
        FilterActions.MODE -> withMode(mode.toggled())
        FilterActions.COMPONENTS -> withMatchComponents(!matchComponents)
        else -> null
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ResourceFilter<*> || other.kind !== kind || other.size != size || other.mode != mode || other.matchComponents != matchComponents) return false
        @Suppress("UNCHECKED_CAST")
        val theirs = other.entries as List<T>
        return entries.indices.all { kind.equal(entries[it], theirs[it]) }
    }

    override fun hashCode(): Int = Objects.hash(size, mode, matchComponents, entries.map(kind::hash))

    override fun toString(): String = "ResourceFilter($mode, components=$matchComponents, ${listed()})"

    companion object {
        /**
         * Saves a filter of [kind] with [size] entries: `entries` (only when one is set), `mode` and
         * `match_components`.
         */
        @JvmStatic
        fun <T : Any> codec(kind: FilterKind<T>, size: Int): Codec<ResourceFilter<T>> = RecordCodecBuilder.create { i ->
            i.group(
                kind.codec.listOf().optionalFieldOf("entries", emptyList()).forGetter { f -> if (f.isEmpty) emptyList() else f.entries },
                Codec.STRING.xmap({ n -> FilterMode.entries.firstOrNull { it.name == n } ?: FilterMode.ALLOW }, FilterMode::name)
                    .optionalFieldOf("mode", FilterMode.ALLOW).forGetter(ResourceFilter<T>::mode),
                Codec.BOOL.optionalFieldOf("match_components", true).forGetter(ResourceFilter<T>::matchComponents),
            ).apply(i) { entries, mode, match -> ResourceFilter(kind, size, entries, mode, match) }
        }
    }
}

/**
 * Actions on a [ResourceFilter] from a screen, packed in an int: an operation and, for [SET], a cell. Menus put it in a
 * [com.itszuvalex.itszulib.menu.MenuActionPayload]'s data next to their own bits (e.g. which filter, shifted above
 * [BITS]) and hand it to [ResourceFilter.apply] with the carried stack.
 */
object FilterActions {
    /** Set cell to the held thing (an empty hand clears it). */
    const val SET = 0

    /** Toggle allow / deny. */
    const val MODE = 1

    /** Toggle matching data components. */
    const val COMPONENTS = 2

    /** Bits an action takes: 2 for the operation, 6 for the cell (up to 64 cells). */
    const val BITS = 8

    @JvmStatic
    fun set(cell: Int): Int = (cell and 0x3F) shl 2 or SET

    @JvmStatic
    fun mode(): Int = MODE

    @JvmStatic
    fun components(): Int = COMPONENTS

    @JvmStatic
    fun op(action: Int): Int = action and 0x3

    @JvmStatic
    fun cell(action: Int): Int = (action ushr 2) and 0x3F
}

/** ItszuLib's [FilterKind]s. */
object FilterKinds {
    /** Item stacks: the same item, and with component matching the same components. A click sets the held item. */
    @JvmField
    val ITEM: FilterKind<ItemStack> = object : FilterKind<ItemStack> {
        override val empty: ItemStack get() = ItemStack.EMPTY
        override val codec: Codec<ItemStack> = ItemStack.OPTIONAL_CODEC
        override fun isEmpty(t: ItemStack) = t.isEmpty
        override fun same(a: ItemStack, b: ItemStack, components: Boolean) =
            if (components) ItemStack.isSameItemSameComponents(a, b) else ItemStack.isSameItem(a, b)
        override fun entryOf(t: ItemStack): ItemStack = t.copyWithCount(1)
        override fun fromHeld(held: ItemStack): ItemStack = if (held.isEmpty) ItemStack.EMPTY else held.copyWithCount(1)
        override fun equal(a: ItemStack, b: ItemStack) = ItemStack.matches(a, b)
        override fun hash(t: ItemStack) = ItemStack.hashItemAndComponents(t)
    }

    /**
     * ItszuLib item stacks ([com.itszuvalex.itszulib.api.adapters.IItemStack]): the same item id, and with component
     * matching the same components. For storages and indexes ([com.itszuvalex.itszulib.api.storage.ItemStorageIndex])
     * and for tests without a game.
     */
    @JvmField
    val I_ITEM: FilterKind<com.itszuvalex.itszulib.api.adapters.IItemStack> = object : FilterKind<com.itszuvalex.itszulib.api.adapters.IItemStack> {
        override val empty: com.itszuvalex.itszulib.api.adapters.IItemStack get() = com.itszuvalex.itszulib.api.adapters.IItemStack.Empty
        override val codec: Codec<com.itszuvalex.itszulib.api.adapters.IItemStack> get() = com.itszuvalex.itszulib.api.adapters.IItemStack.codec()
        override fun isEmpty(t: com.itszuvalex.itszulib.api.adapters.IItemStack) = t.isEmpty()
        override fun same(a: com.itszuvalex.itszulib.api.adapters.IItemStack, b: com.itszuvalex.itszulib.api.adapters.IItemStack, components: Boolean) =
            a.item() == b.item() && (!components || a.components() == b.components())
        override fun entryOf(t: com.itszuvalex.itszulib.api.adapters.IItemStack) = t.copy().also { it.setStackSize(1) }
        override fun fromHeld(held: ItemStack): com.itszuvalex.itszulib.api.adapters.IItemStack = com.itszuvalex.itszulib.api.adapters.IItemStack.of(held.copyWithCount(1))
        override fun equal(a: com.itszuvalex.itszulib.api.adapters.IItemStack, b: com.itszuvalex.itszulib.api.adapters.IItemStack) =
            (a.isEmpty() && b.isEmpty()) || (same(a, b, true) && a.stackSize() == b.stackSize())
        override fun hash(t: com.itszuvalex.itszulib.api.adapters.IItemStack) = if (t.isEmpty()) 0 else Objects.hash(t.item(), t.components())
    }

    /**
     * Fluid stacks: the same fluid, and with component matching the same components. A click sets the fluid in the
     * held container (a bucket, a tank item).
     */
    @JvmField
    val FLUID: FilterKind<FluidStack> = object : FilterKind<FluidStack> {
        override val empty: FluidStack get() = FluidStack.EMPTY
        override val codec: Codec<FluidStack> = FluidStack.OPTIONAL_CODEC
        override fun isEmpty(t: FluidStack) = t.isEmpty
        override fun same(a: FluidStack, b: FluidStack, components: Boolean) =
            if (components) FluidStack.isSameFluidSameComponents(a, b) else FluidStack.isSameFluid(a, b)
        override fun entryOf(t: FluidStack): FluidStack = t.copyWithAmount(1)
        override fun fromHeld(held: ItemStack): FluidStack? =
            if (held.isEmpty) FluidStack.EMPTY else FluidUtil.getFluidContained(held).orElse(null)?.takeUnless { it.isEmpty }?.copyWithAmount(1)
        override fun equal(a: FluidStack, b: FluidStack) = FluidStack.matches(a, b)
        override fun hash(t: FluidStack) = FluidStack.hashFluidAndComponents(t)
    }
}
