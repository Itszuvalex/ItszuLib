package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.storage.IFluidStorage
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.{IFluidHandler, IFluidTankProperties}

class WrapperFluidStorageIFluidHandler(val storage: IFluidStorage) extends IFluidHandler {
  override def getTankProperties: Array[IFluidTankProperties] = Array(Converter.IFluidTankPropertiesFromIFluidStorage(storage))

  override def fill(resource: FluidStack, doFill: Boolean): Int = storage.fill(Converter.IFluidStackFromFluidStack(resource), doFill)

  override def drain(resource: FluidStack, doDrain: Boolean): FluidStack = Converter.FluidStackFromIFluidStack(storage.drain(Converter.IFluidStackFromFluidStack(resource), doDrain))

  override def drain(maxDrain: Int, doDrain: Boolean): FluidStack = Converter.FluidStackFromIFluidStack(storage.drainIStack(maxDrain, doDrain))
}
