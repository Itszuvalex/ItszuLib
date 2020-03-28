package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.{IFluidStack, WrapperVanillaFluidStack}
import net.minecraftforge.fluids.capability.IFluidTankProperties

trait IFluidStorageProperties extends IFluidTankProperties {
  def canDrainFluidType(fluidStack: IFluidStack): Boolean = canDrainFluidType(fluidStack.toMinecraft)

  def canFillFluidType(fluidStack: IFluidStack): Boolean = canFillFluidType(fluidStack.toMinecraft)

  def getContentsIStack: IFluidStack = WrapperVanillaFluidStack(getContents)
}
