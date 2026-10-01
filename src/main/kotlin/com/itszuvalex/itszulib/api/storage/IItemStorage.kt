package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.utility.MCConstants
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.neoforge.common.util.ValueIOSerializable
import kotlin.math.min

/**
 * Slot-based item storage with default transfer logic. Persisted as one entry per non-empty slot, keyed by the slot
 * index ("0", "1", ...), encoded with [IItemStack.codec].
 */
interface IItemStorage : ValueIOSerializable {
    fun get(index: Int): IItemStack

    fun size(): Int

    /**
     * Replaces a slot. Storages with a change listener notify it (see [setChanged]).
     */
    fun setSlot(index: Int, stack: IItemStack)

    /**
     * Replaces a slot without notifying any change listener. For callers that batch changes and call [setChanged]
     * themselves, e.g. inside a NeoForge transaction, where side effects must wait for commit.
     */
    fun setSlotQuietly(index: Int, stack: IItemStack) = setSlot(index, stack)

    fun canInsert(index: Int, stack: IItemStack): Boolean = true

    fun maxStackSize(index: Int): Int = min(MCConstants.ITEMSTACK_MAX, get(index).stackSizeMax())

    /**
     * Removes up to [amount] items from the slot.
     *
     * @return The removed items; [IItemStack.Empty] if [amount] is not positive (1.12.2 grew the slot for a negative
     * amount).
     */
    fun split(index: Int, amount: Int): IItemStack {
        if (amount <= 0) return IItemStack.Empty
        val slot = get(index)
        val ret = slot.copy()
        if (amount >= slot.stackSize()) {
            setSlot(index, IItemStack.Empty)
        } else {
            slot.modifyStackSize(-amount)
            setSlot(index, slot) // Trigger updates
            ret.setStackSize(amount)
        }
        return ret
    }

    /**
     * Inserts as much of [stack] as fits, if [canInsert] accepts it (1.12.2 never checked). A storage's owner filling
     * slots that refuse outside insertion (e.g. output slots) uses [insertUnchecked].
     *
     * @param index Index to insert into
     * @param stack Stack to insert
     * @return IItemStack containing the leftovers from stack; all of it if [canInsert] refuses.
     */
    fun insert(index: Int, stack: IItemStack): IItemStack {
        if (stack.isEmpty() || !canInsert(index, stack)) return stack
        return insertUnchecked(index, stack)
    }

    /**
     * [insert] without the [canInsert] check, for a storage's owner filling its own slots. Code acting for a player or
     * another block uses [insert].
     */
    fun insertUnchecked(index: Int, stack: IItemStack): IItemStack {
        if (stack.isEmpty()) return stack

        val max = min(stack.stackSizeMax(), maxStackSize(index))
        val slot = get(index)
        if (slot.isEmpty()) {
            if (stack.stackSize() <= max) {
                // Copy so the caller's later mutations can't reach into this storage.
                setSlot(index, stack.copy())
                return IItemStack.Empty
            }

            val sc = stack.copy()
            sc.setStackSize(max)
            val ret = stack.copy()
            ret.modifyStackSize(-max)
            setSlot(index, sc)
            return ret
        }

        if (slot.isItemEqual(stack)) {
            val room = max - slot.stackSize()
            // The slot may already hold more than this insert allows (e.g. a smaller stack limit); insert nothing.
            if (room <= 0) return stack
            if (stack.stackSize() <= room) {
                slot.modifyStackSize(stack.stackSize())
                setSlot(index, slot)
                return IItemStack.Empty
            }

            val slotcopy = slot.copy()
            slotcopy.modifyStackSize(room)
            val ret = stack.copy()
            ret.modifyStackSize(-room)
            setSlot(index, slotcopy)
            return ret
        }

        return stack
    }

    /**
     * Moves up to [amount] items through [storage]'s [insert], so slots whose [canInsert] refuses them receive nothing.
     * Safe when [storage] is this storage or a view of it, even for the same slot: the source is re-read after the
     * insert. (1.12.2 wrote back a copy taken before the insert, which destroyed the items moved into the same slot.)
     *
     * @return Amount of [amount] that was not transferred.
     */
    fun transferSlotIntoStorageSlot(slot: Int, storage: IItemStorage, targetSlot: Int, amount: Int): Int {
        if (amount <= 0) return amount
        val source = get(slot)
        if (source.isEmpty()) return amount
        val moving = source.copy()
        moving.setStackSize(min(moving.stackSize(), amount))
        val transferred = moving.stackSize() - storage.insert(targetSlot, moving).stackSize()
        if (transferred <= 0) return amount
        // Re-read: the insert may have changed this slot (same storage and slot, or a view of it).
        val after = get(slot).copy()
        after.modifyStackSize(-transferred)
        setSlot(slot, if (after.stackSize() <= 0) IItemStack.Empty else after)
        return amount - transferred
    }

    /**
     * Tops up matching non-empty slots first, then fills empty ones.
     *
     * @return Amount of [amount] that was not transferred.
     */
    fun transferSlotIntoStorage(slot: Int, storage: IItemStorage, amount: Int): Int {
        var transferRemaining = amount
        for (i in 0 until storage.size()) {
            if (storage.get(i).isEmpty() || !storage.canInsert(i, get(slot))) continue
            transferRemaining = transferSlotIntoStorageSlot(slot, storage, i, transferRemaining)
            if (transferRemaining <= 0) return 0
        }
        if (get(slot).isEmpty()) return transferRemaining

        for (i in 0 until storage.size()) {
            if (!storage.get(i).isEmpty() || !storage.canInsert(i, get(slot))) continue
            transferRemaining = transferSlotIntoStorageSlot(slot, storage, i, transferRemaining)
            if (transferRemaining <= 0) break
        }
        return transferRemaining
    }

    /**
     * @return Amount of [amount] that was not transferred.
     */
    fun transferIntoStorage(storage: IItemStorage, amount: Int): Int {
        if (storage === this) return amount

        var transferRemaining = amount
        for (i in 0 until size()) {
            if (get(i).isEmpty()) continue
            transferRemaining = transferSlotIntoStorage(i, storage, transferRemaining)
            if (transferRemaining <= 0) break
        }
        return transferRemaining
    }

    /**
     * Replaces every slot. [serialize] omits empty slots, so a missing slot is cleared rather than kept.
     */
    override fun deserialize(input: ValueInput) {
        for (i in 0 until size()) setSlot(i, input.read(i.toString(), IItemStack.codec()).orElse(IItemStack.Empty))
    }

    override fun serialize(output: ValueOutput) {
        for (i in 0 until size()) {
            val stack = get(i)
            if (!stack.isEmpty()) output.store(i.toString(), IItemStack.codec(), stack)
        }
    }

    fun isEmpty(): Boolean = (0 until size()).all { get(it).isEmpty() }

    /**
     * Notifies the storage's change listener, if any (e.g. the owning block entity's setChanged).
     */
    fun setChanged() {}

    companion object {
        @JvmField
        val Empty: IItemStorage = object : IItemStorage {
            override fun get(index: Int): IItemStack = IItemStack.Empty
            override fun size(): Int = 0
            override fun setSlot(index: Int, stack: IItemStack) {}
        }
    }
}
