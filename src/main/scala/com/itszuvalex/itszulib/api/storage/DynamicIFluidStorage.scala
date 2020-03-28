package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.NBTTagCompound

class DynamicIFluidStorage(val getter: () => IFluidStorage) extends IFluidStorage {
  override def deserializeNBT(nbt: NBTTagCompound): Unit = getter().deserializeNBT(nbt)

  override def fill(resource: IFluidStack, doFill: Boolean): Int = getter().fill(resource, doFill)

  override def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = getter().drain(resource, doDrain)

  override def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = getter().drainIStack(maxDrain, doDrain)

  override def length: Int = getter().length

  override def apply(idx: Int): IFluidStack = getter().apply(idx)

  override def serializeNBT(): NBTTagCompound = getter().serializeNBT()

  override def canFillFluidType(index: Int, resource: IFluidStack): Boolean = getter().canFillFluidType(index, resource)

  override def canDrainFluidType(index: Int, resource: IFluidStack): Boolean = getter().canDrainFluidType(index, resource)

  override def canDrain(index: Int): Boolean = getter().canDrain(index)

  override def canFill(index: Int): Boolean = getter().canFill(index)

  override def capacity(index: Int): Int = getter().capacity(index)
}
