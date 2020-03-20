package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.NBTTagCompound

class DynamicIFluidStorage(val getter: () => IFluidStorage) extends IFluidStorage {
  override def deserializeNBT(nbt: NBTTagCompound): Unit = getter().deserializeNBT(nbt)

  override def fill(resource: IFluidStack, doFill: Boolean): Int = getter().fill(resource, doFill)

  override def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = getter().drain(resource, doDrain)

  override def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = getter().drainIStack(maxDrain, doDrain)

  override def contents: IFluidStack = getter().contents

  override def capacity: Int = getter().capacity

  override def serializeNBT(): NBTTagCompound = getter().serializeNBT()

  override def canFillFluidType(resource: IFluidStack): Boolean = getter().canFillFluidType(resource)

  override def canDrainFluidType(resource: IFluidStack): Boolean = getter().canDrainFluidType(resource)

  override def canDrain: Boolean = getter().canDrain

  override def canFill: Boolean = getter().canFill

  override def markDirty(): Unit = getter().markDirty()
}
