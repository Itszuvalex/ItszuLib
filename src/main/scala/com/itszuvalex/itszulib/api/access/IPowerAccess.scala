package com.itszuvalex.itszulib.api.access

/**
  * Created by Christopher Harris (Itszuvalex) on 3/12/2016.
  */
trait IPowerAccess extends IAccess[IPowerAccess, Double] {

  def maxStorage: Option[Double]

  /**
    * Copies all required info from another A
    *
    * @param other
    */
  override def copyFromAccess(other: IPowerAccess, deepCopy: Boolean): Unit = {
    set(other.get.getOrElse(0d))
  }

  def increment(amt: Double): Double = get.map { cur =>
    val amount = Math.min(amt, room.get)
    set(cur + amount)
    onChanged()
    amount
                                               }.getOrElse(0)

  def room = maxStorage.map(_ - get.get)

  def decrement(amt: Double): Double = get.map { cur =>
    val amount = Math.min(amt, cur)
    set(cur - amount)
    onChanged()
    amount
                                               }.getOrElse(0)

  def onChanged(): Unit = {}

  def clear(): Unit = {
    set(0)
  }

  def isValid: Boolean
}
