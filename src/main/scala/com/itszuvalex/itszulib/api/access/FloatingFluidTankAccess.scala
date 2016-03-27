package com.itszuvalex.itszulib.api.access

import net.minecraftforge.fluids.{FluidStack, FluidTank}

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class FloatingFluidTankAccess(item: FluidTank) extends IFluidTankAccess {

  private[access] var backingFluid = Option(item)

  def this(fluid: FluidStack, capacity: Int) =
    this({
           val tank = new FluidTank(capacity)
           tank.setFluid(fluid)
           tank
         })


  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing FluidStack
    */
  override def get: Option[FluidTank] = backingFluid

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = true

  /**
    * Sets this item access's storage to the FluidStack.
    *
    * @param stack FluidStack to set this to.
    */
  override def set(stack: FluidTank): Unit = {
    backingFluid = Option(stack)
  }
}
