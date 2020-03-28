package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.NBTTagCompound

class DynamicIFluidStorageModifiable(val gettermod: () => IFluidStorageModifiable) extends IFluidStorageModifiable {

  /**
    *
    * @param resource
    * @param doFill
    * @return Amount of resource filled
    */
  override def fill(resource: IFluidStack, doFill: Boolean): Int = gettermod().fill(resource, doFill)

  /**
    *
    * @param resource
    * @param doDrain
    * @return Amount of resource drained
    */
  override def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = gettermod().drain(resource, doDrain)

  override def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = gettermod().drainIStack(maxDrain, doDrain)

  override def writeFluidToNBT(nbt: NBTTagCompound, fluid: IFluidStack, slot: Int): Unit = gettermod().writeFluidToNBT(nbt, fluid, slot)

  override def readFluidFromSlot(NBTTagCompound: NBTTagCompound, slot: Int): IFluidStack = gettermod().readFluidFromSlot(NBTTagCompound, slot)

  override def canFillFluidType(index: Int, resource: IFluidStack): Boolean = gettermod().canFillFluidType(index, resource)

  override def canDrainFluidType(index: Int, resource: IFluidStack): Boolean = gettermod().canDrainFluidType(index, resource)

  override def canDrain(index: Int): Boolean = gettermod().canDrain(index)

  override def canFill(index: Int): Boolean = gettermod().canFill(index)

  override def capacity(index: Int): Int = gettermod().capacity(index)

  override def update(i: Int, s: IFluidStack): Unit = gettermod().update(i, s)

  override def length: Int = gettermod().length

  override def apply(idx: Int): IFluidStack = gettermod().apply(idx)

  override def serializeNBT(): NBTTagCompound = gettermod().serializeNBT()

  override def deserializeNBT(nbt: NBTTagCompound): Unit = gettermod().deserializeNBT(nbt)
}
