package com.itszuvalex.itszulib.api.access

import net.minecraftforge.fluids.FluidStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/12/2016.
  */
trait IFluidAccess {

  def getFluid: Option[FluidStack]

  def setFluid(fluid: FluidStack): Unit

  def isEmpty = getFluid.isEmpty

  def isDefined = getFluid.isDefined

  def matches(matcher: (FluidStack) => Boolean): Boolean = matcher != null && matcher(getFluid.orNull)

  def isFluidValid(fluid: FluidStack): Boolean = true

  def increment(amt: Int): Int = getFluid.map { i =>
    if (amt < 0) return 0

    val amount = Math.min(room.get, amt)
    i.amount += amount
    onChanged()
    amount
                                              }.getOrElse(0)

  def room = maxStorage.map(_ - currentStorage.get)

  def maxStorage: Option[Int] = getFluid.map(_ => Int.MaxValue)

  def decrement(amt: Int): Int = getFluid.map { i =>
    if (amt < 0) return 0

    val amount = Math.min(currentStorage.get, amt)
    i.amount -= amount
    if (i.amount <= 0)
      clear()
    onChanged()
    amount
                                              }.getOrElse(0)

  def currentStorage: Option[Int] = getFluid.map(_.amount)

  def onChanged() = {}

  def clear() = {
    setFluid(null)
  }

  def split(int: Int): IFluidAccess

  def copyFromAccess(other: IFluidAccess, copyStack: Boolean = true): Unit =
    setFluid(other.getFluid match {
               case Some(i) => if (copyStack) i.copy() else i
               case None => null
             })

  def isValid: Boolean
}
