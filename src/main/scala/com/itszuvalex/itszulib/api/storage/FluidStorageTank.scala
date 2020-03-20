package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.{Converter, IFluidStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.capability.{IFluidHandler, IFluidTankProperties}
import net.minecraftforge.fluids.{FluidStack, FluidTank}

class FluidStorageTank(private val tank: FluidTank) extends IFluidStorage with IFluidHandler {

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

  override def contents: IFluidStack = Converter.IFluidStackFromFluidStack(tank.getFluid)

  override def capacity: Int = tank.getCapacity

  override def canDrainFluidType(resource: IFluidStack): Boolean = tank.canDrainFluidType(Converter.FluidStackFromIFluidStack(resource))

  override def canFillFluidType(resource: IFluidStack): Boolean = tank.canFillFluidType(Converter.FluidStackFromIFluidStack(resource))

  override def canDrain: Boolean = tank.canDrain

  override def canFill: Boolean = tank.canFill
}
