package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraftforge.fluids.Fluid

/**
  * Created by Christopher Harris (Itszuvalex) on 3/12/2016.
  */
trait IFluidAccess extends IAccess[IFluidAccess, IFluidStack] {

  def getFluid: Option[Fluid] = get.map(_.fluid)

  def increment(amt: Int): Int = get.map { i =>
    if (amt < 0) return 0

    val amount = Math.min(room.get, amt)
    i.amount += amount
    onChanged()
    amount
                                         }.getOrElse(0)

  def room = maxStorage.map(_ - currentStorage.get)

  def maxStorage: Option[Int] = get.map(_ => Int.MaxValue)

  def currentStorage: Option[Int] = get.map(_.amount)

  def copyFromAccess(other: IFluidAccess, copyStack: Boolean = true): Unit =
    set(other.get match {
          case Some(i) => if (copyStack) i.copy() else i
          case None => null
        })

  def split(amount: Int): IFluidAccess = amount match {
    case invalid if invalid <= 0 => new FloatingFluidAccess(null)
    case _ => new FloatingFluidAccess(
                                       get.map { f =>
                                         val fluid = f.copy()
                                         fluid.amount = decrement(amount)
                                         if (fluid.amount > 0)
                                           fluid
                                         else null
                                               }.orNull
                                     )
  }

  def decrement(amt: Int): Int = get.map { i =>
    if (amt < 0) return 0

    val amount = Math.min(currentStorage.get, amt)
    i.amount -= amount
    if (i.amount <= 0)
      clear()
    onChanged()
    amount
                                         }.getOrElse(0)

  def transfer(other: IFluidAccess, amt: Int): Int = {
    if (isEmpty)
      0
    else if (other.isEmpty) {
      val stack = get.get.copy()
      stack.amount = 0
      if (other.canSetTo(stack)) {
        other.set(stack)
        decrement(other.increment(Math.min(amt, currentStorage.get)))
      }
      else 0
    }
    else if (other.get.exists(_.isFluidEqual(get.orNull))) {
      decrement(other.increment(Math.min(amt, currentStorage.get)))
    }
    else 0
  }

  def clear() = {
    set(null)
  }

  def isValid: Boolean
}
