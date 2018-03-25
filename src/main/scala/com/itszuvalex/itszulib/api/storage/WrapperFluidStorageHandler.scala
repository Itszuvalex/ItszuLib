package com.itszuvalex.itszulib.api.storage

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.{IFluidHandler, IFluidTankProperties}

class WrapperFluidStorageHandler(private val handler: IFluidHandler) extends IFluidStorage {
  override def deserializeNBT(nbt: NBTTagCompound): Unit = {}

  override def serializeNBT(): NBTTagCompound = new NBTTagCompound

  override def fill(resource: FluidStack, doFill: Boolean): Int = handler.fill(resource, doFill)

  override def drain(resource: FluidStack, doDrain: Boolean): FluidStack = handler.drain(resource, doDrain)

  override def drain(maxDrain: Int, doDrain: Boolean): FluidStack = handler.drain(maxDrain, doDrain)

  override def getTankProperties: Array[IFluidTankProperties] = handler.getTankProperties
}
