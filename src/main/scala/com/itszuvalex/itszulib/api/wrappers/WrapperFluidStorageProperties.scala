package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.storage.IFluidStorageProperties
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidTankProperties

case class WrapperFluidStorageProperties(private val tankProperty: IFluidTankProperties) extends IFluidStorageProperties {
  override def canDrainFluidType(fluidStack: FluidStack): Boolean = tankProperty.canDrainFluidType(fluidStack)

  override def canFill: Boolean = tankProperty.canFill

  override def canDrain: Boolean = tankProperty.canDrain

  override def canFillFluidType(fluidStack: FluidStack): Boolean = tankProperty.canFillFluidType(fluidStack)

  override def getContents: FluidStack = tankProperty.getContents

  override def getCapacity: Int = tankProperty.getCapacity
}
