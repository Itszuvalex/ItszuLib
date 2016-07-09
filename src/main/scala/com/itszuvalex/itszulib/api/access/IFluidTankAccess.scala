package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.wrappers.WrappedFluidStack
import net.minecraftforge.fluids.{Fluid, FluidTank}

/**
  * Created by Christopher Harris (Itszuvalex) on 3/12/2016.
  */
trait IFluidTankAccess extends IAccess[IFluidTankAccess, FluidTank] {

  def getFluid: Option[Fluid] = get.map(_.getFluid).map(_.getFluid)

  def increment(amt: Int): Int = get.map { i =>
    if (amt < 0) return 0

    val amount = new FloatingFluidAccess(WrappedFluidStack(i.getFluid)).increment(amt)
    onChanged()
    amount
                                         }.getOrElse(0)

  def room = maxStorage.map(_ - currentStorage.get)

  def currentStorage: Option[Int] = get.map(_.getFluidAmount)

  def copyFromAccess(other: IFluidTankAccess, copyStack: Boolean = true): Unit =
    set(other.get match {
          case Some(i) => if (copyStack) i else i
          case None => null
        })

  def split(amount: Int): IFluidTankAccess = amount match {
    case invalid if invalid <= 0 => new FloatingFluidTankAccess(null)
    case _ => new FloatingFluidTankAccess(
                                           get.map(_.drain(amount, true)).orNull,
                                           maxStorage.getOrElse(0)
                                         )
  }

  def maxStorage: Option[Int] = get.map(_.getCapacity)

  def decrement(amt: Int): Int = get.map { i =>
    if (amt < 0) return 0

    val stack = i.drain(amt, true)
    onChanged()
    if (stack == null)
      0
    else stack.amount
                                         }.getOrElse(0)

  def clear() = {
    set(null)
  }

  def isValid: Boolean
}
