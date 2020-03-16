package com.itszuvalex.itszulib.api.storage

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidTankProperties

class DynamicIFluidStorage(val getter: () => IFluidStorage) extends IFluidStorage {
  override def deserializeNBT(nbt: NBTTagCompound): Unit = getter().deserializeNBT(nbt)

  override def serializeNBT(): NBTTagCompound = getter().serializeNBT()

  override def fill(resource: FluidStack, doFill: Boolean): Int = getter().fill(resource, doFill)

  override def drain(resource: FluidStack, doDrain: Boolean): FluidStack = getter().drain(resource, doDrain)

  override def drain(maxDrain: Int, doDrain: Boolean): FluidStack = getter().drain(maxDrain, doDrain)

  override def getTankProperties: Array[IFluidTankProperties] = getter().getTankProperties
}
