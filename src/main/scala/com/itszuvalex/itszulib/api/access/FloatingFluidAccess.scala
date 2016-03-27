package com.itszuvalex.itszulib.api.access

import net.minecraftforge.fluids.FluidStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class FloatingFluidAccess(fluid: FluidStack) extends IFluidAccess {
  private[access] var backingFluid = Option(fluid)

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = true

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing FluidStack
    */
  override def get: Option[FluidStack] = backingFluid

  /**
    * Sets this item access's storage to the FluidStack.
    *
    * @param stack FluidStack to set this to.
    */
  override def set(stack: FluidStack): Unit = {
    backingFluid = Option(stack)
  }
}
