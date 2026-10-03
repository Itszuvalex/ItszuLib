package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.adapters.IFluidStack
import com.itszuvalex.itszulib.api.adapters.IItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.TreeSet
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Told when a key (an item or fluid id) first appears in, or last leaves, an indexed storage, so an index across
 * several storages ([ItemStorageIndex], [FluidStorageIndex]) knows which storages to ask.
 */
fun interface IndexListener {
    fun changed(key: Identifier, present: Boolean)
}

/**
 * Keys to slots, kept current one slot at a time, with the empty slots and listeners for keys appearing and leaving.
 * Shared by [IndexedItemStorage] and [IndexedFluidStorage].
 */
class SlotIndex(private val keyOf: (Int) -> Identifier?) {
    private val slotsByKey = HashMap<Identifier, TreeSet<Int>>()
    private var keys = arrayOfNulls<Identifier>(0)
    private val empty = TreeSet<Int>()
    private val listeners = CopyOnWriteArrayList<IndexListener>()

    val size: Int get() = keys.size

    fun addListener(listener: IndexListener) {
        listeners += listener
    }

    fun removeListener(listener: IndexListener) {
        listeners -= listener
    }

    fun slotsOf(key: Identifier): Set<Int> = slotsByKey[key] ?: emptySet()

    fun keys(): Set<Identifier> = slotsByKey.keys

    fun emptySlots(): Set<Int> = empty

    /** Re-reads every slot of a storage of [size] slots. */
    fun rebuild(size: Int) {
        val before = slotsByKey.keys.toSet()
        slotsByKey.clear()
        empty.clear()
        keys = arrayOfNulls(size)
        for (slot in 0 until size) index(slot)
        for (key in before - slotsByKey.keys) listeners.forEach { it.changed(key, false) }
        for (key in slotsByKey.keys - before) listeners.forEach { it.changed(key, true) }
    }

    /** Re-reads [slot] after it changed. */
    fun update(slot: Int) {
        if (slot !in keys.indices) return
        val old = keys[slot]
        val new = keyOf(slot)
        if (old == new) {
            if (new == null) empty += slot
            return
        }
        if (old != null) {
            val slots = slotsByKey.getValue(old)
            slots -= slot
            if (slots.isEmpty()) {
                slotsByKey -= old
                listeners.forEach { it.changed(old, false) }
            }
        } else {
            empty -= slot
        }
        keys[slot] = null
        index(slot)
        if (new != null && slotsByKey.getValue(new).size == 1) listeners.forEach { it.changed(new, true) }
    }

    private fun index(slot: Int) {
        val key = keyOf(slot)
        keys[slot] = key
        if (key == null) empty += slot else slotsByKey.getOrPut(key, ::TreeSet) += slot
    }
}

/**
 * An item storage that knows where everything is: every write through it updates an index of item ids to slots and of
 * empty slots, so finding, counting, inserting and extracting an item touch only the slots that matter, never the
 * whole storage. Wrap a storage that only it writes to (the NeoForge adapters, menus and automation all write through
 * the [IItemStorage] they are given); if something changes [inner] behind its back, call [slotChanged] or [rebuild].
 *
 * The index knows item ids only: slots of one item may hold stacks with different components, which [count] and
 * [extract] tell apart through a matcher.
 */
open class IndexedItemStorage(val inner: IItemStorage) : IItemStorage {
    private val index = SlotIndex { slot -> inner.get(slot).takeUnless { it.isEmpty() }?.item() }

    init {
        index.rebuild(inner.size())
    }

    fun addListener(listener: IndexListener) = index.addListener(listener)

    fun removeListener(listener: IndexListener) = index.removeListener(listener)

    /** Slots holding [item]. */
    fun slotsOf(item: Identifier): Set<Int> {
        checkSize()
        return index.slotsOf(item)
    }

    /** The items held. */
    fun items(): Set<Identifier> {
        checkSize()
        return index.keys()
    }

    fun contains(item: Identifier): Boolean = slotsOf(item).isNotEmpty()

    /** Empty slots. */
    fun emptySlots(): Set<Int> {
        checkSize()
        return index.emptySlots()
    }

    /** How many of [item] are held (only stacks [matching], e.g. with the same components). */
    @JvmOverloads
    fun count(item: Identifier, matching: (IItemStack) -> Boolean = { true }): Int =
        slotsOf(item).sumOf { slot -> get(slot).takeIf(matching)?.stackSize() ?: 0 }

    /** Slots holding an item in [tag] (looked up among the items held, not the slots). */
    fun slotsOfTag(tag: TagKey<Item>): Set<Int> = items().filter { id -> isInTag(id, tag) }.flatMapTo(HashSet()) { slotsOf(it) }

    /** Rebuilds the whole index (after [inner] changed behind this storage's back). */
    fun rebuild() = index.rebuild(inner.size())

    /** Re-reads [slot] (after it changed behind this storage's back). */
    fun slotChanged(slot: Int) {
        checkSize()
        index.update(slot)
    }

    private fun checkSize() {
        if (index.size != inner.size()) index.rebuild(inner.size())
    }

    /**
     * Inserts as much of [stack] as fits: first into slots already holding its item, then into empty slots. Honours
     * [canInsert].
     *
     * @return What did not fit.
     */
    fun insert(stack: IItemStack): IItemStack {
        var left = stack
        for (slot in slotsOf(stack.item()).toList()) {
            if (left.isEmpty()) return left
            left = insert(slot, left)
        }
        for (slot in emptySlots().toList()) {
            if (left.isEmpty()) return left
            left = insert(slot, left)
        }
        return left
    }

    /**
     * Takes up to [amount] of [item] (stacks [matching] only; the first matching stack decides components, so one
     * call never mixes stacks that do not stack together).
     *
     * @return What was taken.
     */
    @JvmOverloads
    fun extract(item: Identifier, amount: Int, matching: (IItemStack) -> Boolean = { true }): IItemStack {
        if (amount <= 0) return IItemStack.Empty
        var taken: IItemStack = IItemStack.Empty
        for (slot in slotsOf(item).toList()) {
            val stack = get(slot)
            if (stack.isEmpty() || !matching(stack)) continue
            if (!taken.isEmpty() && !taken.isItemEqual(stack)) continue
            val part = split(slot, amount - taken.stackSize())
            if (part.isEmpty()) continue
            if (taken.isEmpty()) taken = part else taken.setStackSize(taken.stackSize() + part.stackSize())
            if (taken.stackSize() >= amount) break
        }
        return taken
    }

    override fun get(index: Int): IItemStack = inner.get(index)

    override fun size(): Int = inner.size()

    override fun isEmpty(): Boolean = emptySlots().size == size()

    override fun setSlot(index: Int, stack: IItemStack) {
        inner.setSlot(index, stack)
        slotChanged(index)
    }

    override fun setSlotQuietly(index: Int, stack: IItemStack) {
        inner.setSlotQuietly(index, stack)
        slotChanged(index)
    }

    override fun canInsert(index: Int, stack: IItemStack): Boolean = inner.canInsert(index, stack)

    override fun maxStackSize(index: Int): Int = inner.maxStackSize(index)

    override fun setChanged() = inner.setChanged()

    override fun serialize(output: ValueOutput) = inner.serialize(output)

    override fun deserialize(input: ValueInput) {
        inner.deserialize(input)
        rebuild()
    }

    companion object {
        /** Whether item [id] is in [tag] (false for ids not in the item registry). */
        @JvmStatic
        fun isInTag(id: Identifier, tag: TagKey<Item>): Boolean = BuiltInRegistries.ITEM.get(id).map { it.`is`(tag) }.orElse(false)
    }
}

/**
 * The fluid counterpart of [IndexedItemStorage]: fluid ids to tanks, kept current through every write, so [fill] and
 * [drain] go straight to the tanks that matter.
 */
open class IndexedFluidStorage(val inner: IFluidStorage) : IFluidStorage {
    private val index = SlotIndex { tank -> inner.get(tank).takeUnless { it.isEmpty() }?.fluid() }

    init {
        index.rebuild(inner.size())
    }

    fun addListener(listener: IndexListener) = index.addListener(listener)

    fun removeListener(listener: IndexListener) = index.removeListener(listener)

    fun tanksOf(fluid: Identifier): Set<Int> {
        checkSize()
        return index.slotsOf(fluid)
    }

    fun fluids(): Set<Identifier> {
        checkSize()
        return index.keys()
    }

    fun emptyTanks(): Set<Int> {
        checkSize()
        return index.emptySlots()
    }

    fun contains(fluid: Identifier): Boolean = tanksOf(fluid).isNotEmpty()

    /** How much of [fluid] is held. */
    fun amount(fluid: Identifier): Long = tanksOf(fluid).sumOf { get(it).amount().toLong() }

    fun rebuild() = index.rebuild(inner.size())

    fun tankChanged(tank: Int) {
        checkSize()
        index.update(tank)
    }

    private fun checkSize() {
        if (index.size != inner.size()) index.rebuild(inner.size())
    }

    override fun size(): Int = inner.size()

    override fun get(index: Int): IFluidStack = inner.get(index)

    override fun set(index: Int, stack: IFluidStack) {
        inner.set(index, stack)
        tankChanged(index)
    }

    override fun setQuietly(index: Int, stack: IFluidStack) {
        inner.setQuietly(index, stack)
        tankChanged(index)
    }

    override fun capacity(index: Int): Int = inner.capacity(index)

    override fun canFill(index: Int): Boolean = inner.canFill(index)

    override fun canDrain(index: Int): Boolean = inner.canDrain(index)

    override fun canFillFluidType(index: Int, resource: IFluidStack): Boolean = inner.canFillFluidType(index, resource)

    override fun canDrainFluidType(index: Int, resource: IFluidStack): Boolean = inner.canDrainFluidType(index, resource)

    /** Tanks holding the fluid first, then empty tanks, as [IFluidStorage.fill], through the index. */
    override fun fill(resource: IFluidStack, doFill: Boolean): Int {
        if (resource.isEmpty()) return 0
        val toFill = resource.amount()
        var filled = 0
        for (tank in tanksOf(resource.fluid()).toList() + emptyTanks().toList()) {
            if (filled >= toFill) break
            val stack = get(tank)
            if (!stack.isEmpty() && !resource.isFluidEqual(stack)) continue
            if (!canFillFluidType(tank, resource)) continue
            val amt = minOf(toFill - filled, capacity(tank) - stack.amount())
            if (amt <= 0) continue
            if (doFill) set(tank, (if (stack.isEmpty()) resource else stack).copyWithAmount(stack.amount() + amt))
            filled += amt
        }
        return filled
    }

    /** Drains [resource]'s fluid from the tanks holding it only. */
    override fun drain(resource: IFluidStack, doDrain: Boolean): IFluidStack {
        if (resource.isEmpty()) return IFluidStack.Empty
        var drained = 0
        for (tank in tanksOf(resource.fluid()).toList()) {
            if (drained >= resource.amount()) break
            val stack = get(tank)
            if (stack.isEmpty() || !resource.isFluidEqual(stack) || !canDrainFluidType(tank, stack)) continue
            val amt = minOf(resource.amount() - drained, stack.amount())
            if (doDrain) set(tank, stack.copyWithAmount(stack.amount() - amt))
            drained += amt
        }
        return resource.copyWithAmount(drained)
    }

    override fun serialize(output: ValueOutput) = inner.serialize(output)

    override fun deserialize(input: ValueInput) {
        inner.deserialize(input)
        rebuild()
    }
}

/**
 * Finds items across many [IndexedItemStorage]s without scanning them: it keeps which storages hold each item (told by
 * the storages as items appear and leave), so a lookup asks only those, and each answers from its own index. A
 * logistics network, a wall of vaults, a terminal over several machines.
 */
class ItemStorageIndex {
    private val storages = LinkedHashSet<IndexedItemStorage>()
    private val holders = HashMap<Identifier, LinkedHashSet<IndexedItemStorage>>()
    private val listeners = HashMap<IndexedItemStorage, IndexListener>()

    fun storages(): Set<IndexedItemStorage> = storages

    fun add(storage: IndexedItemStorage) {
        if (!storages.add(storage)) return
        val listener = IndexListener { key, present ->
            if (present) holders.getOrPut(key, ::LinkedHashSet) += storage
            else holders[key]?.let { it -= storage; if (it.isEmpty()) holders -= key }
        }
        listeners[storage] = listener
        storage.addListener(listener)
        for (item in storage.items()) holders.getOrPut(item, ::LinkedHashSet) += storage
    }

    fun remove(storage: IndexedItemStorage) {
        if (!storages.remove(storage)) return
        listeners.remove(storage)?.let(storage::removeListener)
        for (item in storage.items()) holders[item]?.let { it -= storage; if (it.isEmpty()) holders -= item }
    }

    /** The storages holding [item]. */
    fun storagesWith(item: Identifier): Set<IndexedItemStorage> = holders[item] ?: emptySet()

    /** Every item held anywhere. */
    fun items(): Set<Identifier> = holders.keys

    /** How many of [item] are held across the storages. */
    @JvmOverloads
    fun count(item: Identifier, matching: (IItemStack) -> Boolean = { true }): Long = storagesWith(item).sumOf { it.count(item, matching).toLong() }

    /** Every (storage, slot) holding [item]. */
    fun find(item: Identifier): Sequence<Pair<IndexedItemStorage, Int>> = storagesWith(item).asSequence().flatMap { s -> s.slotsOf(item).asSequence().map { s to it } }

    /** Takes up to [amount] of [item] from the storages holding it (stacks [matching] only, one kind per call). */
    @JvmOverloads
    fun extract(item: Identifier, amount: Int, matching: (IItemStack) -> Boolean = { true }): IItemStack {
        var taken: IItemStack = IItemStack.Empty
        for (storage in storagesWith(item).toList()) {
            val left = amount - taken.stackSize()
            if (left <= 0) break
            val part = storage.extract(item, left) { matching(it) && (taken.isEmpty() || taken.isItemEqual(it)) }
            if (part.isEmpty()) continue
            if (taken.isEmpty()) taken = part else taken.setStackSize(taken.stackSize() + part.stackSize())
        }
        return taken
    }

    /** Inserts [stack] into storages already holding its item first, then into any. @return What did not fit. */
    fun insert(stack: IItemStack): IItemStack {
        var left = stack
        for (storage in (storagesWith(stack.item()).toList() + storages.toList()).distinct()) {
            if (left.isEmpty()) break
            left = storage.insert(left)
        }
        return left
    }
}

/**
 * The fluid counterpart of [ItemStorageIndex].
 */
class FluidStorageIndex {
    private val storages = LinkedHashSet<IndexedFluidStorage>()
    private val holders = HashMap<Identifier, LinkedHashSet<IndexedFluidStorage>>()
    private val listeners = HashMap<IndexedFluidStorage, IndexListener>()

    fun storages(): Set<IndexedFluidStorage> = storages

    fun add(storage: IndexedFluidStorage) {
        if (!storages.add(storage)) return
        val listener = IndexListener { key, present ->
            if (present) holders.getOrPut(key, ::LinkedHashSet) += storage
            else holders[key]?.let { it -= storage; if (it.isEmpty()) holders -= key }
        }
        listeners[storage] = listener
        storage.addListener(listener)
        for (fluid in storage.fluids()) holders.getOrPut(fluid, ::LinkedHashSet) += storage
    }

    fun remove(storage: IndexedFluidStorage) {
        if (!storages.remove(storage)) return
        listeners.remove(storage)?.let(storage::removeListener)
        for (fluid in storage.fluids()) holders[fluid]?.let { it -= storage; if (it.isEmpty()) holders -= fluid }
    }

    fun storagesWith(fluid: Identifier): Set<IndexedFluidStorage> = holders[fluid] ?: emptySet()

    fun fluids(): Set<Identifier> = holders.keys

    fun amount(fluid: Identifier): Long = storagesWith(fluid).sumOf { it.amount(fluid) }

    /** Drains [resource] from the storages holding its fluid. */
    fun drain(resource: IFluidStack, doDrain: Boolean): IFluidStack {
        var drained = 0
        for (storage in storagesWith(resource.fluid()).toList()) {
            if (drained >= resource.amount()) break
            drained += storage.drain(resource.copyWithAmount(resource.amount() - drained), doDrain).amount()
        }
        return resource.copyWithAmount(drained)
    }

    /** Fills storages already holding the fluid first, then any. @return The amount filled. */
    fun fill(resource: IFluidStack, doFill: Boolean): Int {
        var filled = 0
        for (storage in (storagesWith(resource.fluid()).toList() + storages.toList()).distinct()) {
            if (filled >= resource.amount()) break
            filled += storage.fill(resource.copyWithAmount(resource.amount() - filled), doFill)
        }
        return filled
    }
}
