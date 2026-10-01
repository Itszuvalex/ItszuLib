package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.adapters.IFluidStack
import com.itszuvalex.itszulib.api.storage.IFluidStorage
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.TransferPreconditions
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal
import net.neoforged.neoforge.transfer.transaction.TransactionContext
import java.util.Objects
import kotlin.math.min

/**
 * [IFluidStack] over a NeoForge [FluidStack]. [toMinecraft] returns the live stack.
 */
class WrapperVanillaFluidStack(private val stack: FluidStack) : IFluidStack {
    override fun fluid(): Identifier = BuiltInRegistries.FLUID.getKey(stack.fluid)

    override fun amount(): Int = stack.amount

    override fun setAmount(amount: Int) {
        stack.amount = amount
    }

    override fun components(): DataComponentPatch = stack.componentsPatch

    override fun toMinecraft(): FluidStack = stack

    override fun isEmpty(): Boolean = stack.isEmpty

    override fun copy(): IFluidStack = IFluidStack.of(stack.copy())

    override fun isFluidEqual(other: IFluidStack): Boolean {
        if (isEmpty() || other.isEmpty()) return isEmpty() == other.isEmpty()
        return FluidStack.isSameFluidSameComponents(stack, other.toMinecraft())
    }

    override fun equals(other: Any?): Boolean =
        other is WrapperVanillaFluidStack && FluidStack.matches(stack, other.stack)

    override fun hashCode(): Int = FluidStack.hashFluidAndComponents(stack) * 31 + stack.amount

    override fun toString(): String = "WrapperVanillaFluidStack[$stack]"
}

/**
 * Exposes an [IFluidStorage] as a NeoForge fluid [ResourceHandler], e.g. for the fluid BLOCK capability. Each tank is
 * one index. Changes inside a transaction are journaled per tank and reverted on abort; the storage's change listener
 * runs once, on root commit.
 */
class WrapperResourceHandlerIFluidStorage private constructor(private val storage: IFluidStorage) : ResourceHandler<FluidResource> {
    private val journals = ArrayList<TankJournal>()
    private val changedJournal = RootCommitJournal { storage.setChanged() }

    private fun journal(index: Int): TankJournal {
        while (journals.size <= index) journals.add(TankJournal(journals.size))
        return journals[index]
    }

    override fun size(): Int = storage.size()

    override fun getResource(index: Int): FluidResource {
        Objects.checkIndex(index, size())
        return FluidResource.of(storage.get(index).toMinecraft())
    }

    override fun getAmountAsLong(index: Int): Long {
        Objects.checkIndex(index, size())
        return storage.get(index).amount().toLong()
    }

    override fun getCapacityAsLong(index: Int, resource: FluidResource): Long {
        Objects.checkIndex(index, size())
        return if (resource.isEmpty || isValid(index, resource)) storage.capacity(index).toLong() else 0L
    }

    override fun isValid(index: Int, resource: FluidResource): Boolean {
        Objects.checkIndex(index, size())
        return storage.canFillFluidType(index, IFluidStack.of(resource, 1))
    }

    override fun insert(index: Int, resource: FluidResource, amount: Int, transaction: TransactionContext): Int {
        Objects.checkIndex(index, size())
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount)
        val current = storage.get(index)
        if (!current.isEmpty() && !resource.matches(current.toMinecraft())) return 0
        if (!isValid(index, resource)) return 0
        val inserted = min(amount, storage.capacity(index) - current.amount())
        if (inserted <= 0) return 0
        update(index, transaction, IFluidStack.of(resource, current.amount() + inserted))
        return inserted
    }

    override fun extract(index: Int, resource: FluidResource, amount: Int, transaction: TransactionContext): Int {
        Objects.checkIndex(index, size())
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount)
        val current = storage.get(index)
        if (current.isEmpty() || !resource.matches(current.toMinecraft())) return 0
        if (!storage.canDrainFluidType(index, current)) return 0
        val extracted = min(amount, current.amount())
        if (extracted <= 0) return 0
        update(index, transaction, current.copyWithAmount(current.amount() - extracted))
        return extracted
    }

    private fun update(index: Int, transaction: TransactionContext, stack: IFluidStack) {
        journal(index).updateSnapshots(transaction)
        changedJournal.updateSnapshots(transaction)
        // Quiet while the transaction is open; the storage is notified once, on root commit.
        storage.setQuietly(index, stack)
    }

    private inner class TankJournal(private val index: Int) : SnapshotJournal<IFluidStack>() {
        override fun createSnapshot(): IFluidStack = storage.get(index).copy()

        override fun revertToSnapshot(snapshot: IFluidStack) = storage.setQuietly(index, snapshot)
    }

    companion object {
        @JvmStatic
        fun of(storage: IFluidStorage): ResourceHandler<FluidResource> = WrapperResourceHandlerIFluidStorage(storage)
    }
}
