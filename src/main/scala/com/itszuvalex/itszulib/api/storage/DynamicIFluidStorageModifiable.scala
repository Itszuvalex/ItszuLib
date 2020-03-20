package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.NBTTagCompound

class DynamicIFluidStorageModifiable(val gettermod: () => IFluidStorageModifiable) extends IFluidStorageModifiable {
  override def update(i: Int, s: IFluidStack): Unit = gettermod().update(i, s)

  override def length: Int = gettermod().length

  override def apply(idx: Int): IFluidStack = gettermod().apply(idx)

  override def serializeNBT(): NBTTagCompound = gettermod().serializeNBT()

  override def deserializeNBT(nbt: NBTTagCompound): Unit = gettermod().deserializeNBT(nbt)
}
