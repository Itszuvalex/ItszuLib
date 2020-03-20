package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.NBTTagCompound

class FluidStorageIFluidStack(private val stack: IFluidStack) extends IFluidStorage {

  override def serializeNBT(): NBTTagCompound = stack.serializeNBT()

  override def deserializeNBT(nbt: NBTTagCompound): Unit = stack.deserializeNBT(nbt)

  override def fill(resource: IFluidStack, doFill: Boolean): Int = {
    if (stack.isEmpty || resource.isFluidEqual(stack)) {
      val amt      = resource.amount
      val room     = stack.amountMax - stack.amount
      val toRemove = Math.min(amt, room)
      if (doFill) {
        resource.amount += toRemove
        markDirty()
      }
      toRemove
    }
    else 0
  }

  override def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = {
    if (stack.isEmpty || !stack.isFluidEqual(resource)) return IFluidStack.Empty
    val amtToDrain = Math.min(resource.amount, stack.amount)
    val copy       = stack.copy()
    if (doDrain) {
      stack.amount -= amtToDrain
      markDirty()
    }
    copy.amount = amtToDrain
    copy
  }

  override def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = {
    if (stack.isEmpty) return IFluidStack.Empty
    val amtToDrain = Math.min(maxDrain, stack.amount)
    val copy       = stack.copy()
    if (doDrain) {
      stack.amount -= amtToDrain
      markDirty()
    }
    copy.amount = amtToDrain
    copy
  }

  override def contents: IFluidStack = stack

  override def capacity: Int = stack.amountMax

  override def canFillFluidType(resource: IFluidStack): Boolean = super.canFillFluidType(resource) && stack.isFluidEqual(resource)

  override def canDrainFluidType(resource: IFluidStack): Boolean = super.canDrainFluidType(resource) && stack.isFluidEqual(resource)
}
