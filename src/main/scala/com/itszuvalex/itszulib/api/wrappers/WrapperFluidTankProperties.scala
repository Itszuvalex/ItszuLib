package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.storage.IFluidStorage
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidTankProperties

class WrapperFluidTankProperties(val storage: IFluidStorage, val index: Int) extends IFluidTankProperties {
  override def getContents: FluidStack = Converter.FluidStackFromIFluidStack(storage(index))

  override def getCapacity: Int = storage.capacity(index)

  override def canFill: Boolean = storage.canFill(index)

  override def canDrain: Boolean = storage.canDrain(index)

  override def canFillFluidType(fluidStack: FluidStack): Boolean = storage.canFillFluidType(index, Converter.IFluidStackFromFluidStack(fluidStack))

  override def canDrainFluidType(fluidStack: FluidStack): Boolean = storage.canDrainFluidType(index, Converter.IFluidStackFromFluidStack(fluidStack))
}
