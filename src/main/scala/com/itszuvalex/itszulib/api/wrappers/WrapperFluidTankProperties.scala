package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.storage.IFluidStorage
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidTankProperties

class WrapperFluidTankProperties(val storage: IFluidStorage) extends IFluidTankProperties {
  override def getContents: FluidStack = Converter.FluidStackFromIFluidStack(storage.contents)

  override def getCapacity: Int = storage.capacity

  override def canFill: Boolean = storage.canFill

  override def canDrain: Boolean = storage.canDrain

  override def canFillFluidType(fluidStack: FluidStack): Boolean = storage.canFillFluidType(Converter.IFluidStackFromFluidStack(fluidStack))

  override def canDrainFluidType(fluidStack: FluidStack): Boolean = storage.canDrainFluidType(Converter.IFluidStackFromFluidStack(fluidStack))
}
