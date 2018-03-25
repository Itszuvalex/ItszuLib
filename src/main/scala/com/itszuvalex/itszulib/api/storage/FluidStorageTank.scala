package com.itszuvalex.itszulib.api.storage

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.capability.IFluidTankProperties
import net.minecraftforge.fluids.{FluidStack, FluidTank}

class FluidStorageTank(private val tank: FluidTank) extends IFluidStorage {

  override def deserializeNBT(nbt: NBTTagCompound): Unit = tank.readFromNBT(nbt)

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    tank.writeToNBT(nbt)
    nbt
  }

  override def fill(resource: FluidStack, doFill: Boolean): Int = tank.fill(resource, doFill)

  override def drain(resource: FluidStack, doDrain: Boolean): FluidStack = tank.drain(resource, doDrain)

  override def drain(maxDrain: Int, doDrain: Boolean): FluidStack = tank.drain(maxDrain, doDrain)

  override def getTankProperties: Array[IFluidTankProperties] = tank.getTankProperties
}
