package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.{Converter, IFluidStack}
import com.itszuvalex.itszulib.util.Debug
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.capability.{IFluidHandler, IFluidTankProperties}
import net.minecraftforge.fluids.{FluidStack, FluidTank}

class FluidStorageTank(private val tank: FluidTank) extends IFluidStorageModifiable with IFluidHandler {
  Debug.assert(tank.getTankProperties.length == 1, "FluidTank shouldn't return more than one TankProperties")

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

  override def fill(resource: IFluidStack, doFill: Boolean): Int = fill(Converter.FluidStackFromIFluidStack(resource), doFill)

  override def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = Converter.IFluidStackFromFluidStack(drain(Converter.FluidStackFromIFluidStack(resource), doDrain))

  override def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = Converter.IFluidStackFromFluidStack(tank.drain(maxDrain, doDrain))

  override def length: Int = tank.getTankProperties.length

  override def apply(idx: Int): IFluidStack = Converter.IFluidStackFromFluidStack(tank.getFluid)

  override def update(i: Int, s: IFluidStack): Unit = tank.setFluid(Converter.FluidStackFromIFluidStack(s))
}
