package com.itszuvalex.itszulib.api.access

/**
  * Created by Christopher Harris (Itszuvalex) on 3/12/2016.
  */
trait IPowerAccess {

  def currentStorage: Double

  def maxStorage: Double

  def setStorage(amt: Double): Unit

  def copyFromAccess(other: IPowerAccess): Unit = {
    setStorage(other.currentStorage)
  }

  def increment(amt: Double): Double = {
    val amount = Math.min(amt, room)
    setStorage(currentStorage + amount)
    onChanged()
    amount
  }

  def room = maxStorage - currentStorage

  def decrement(amt: Double): Double = {
    val amount = Math.min(amt, currentStorage)
    setStorage(currentStorage - amount)
    onChanged()
    amount
  }

  def onChanged(): Unit = {}

  def clear(): Unit = {
    setStorage(0)
  }

  def isValid: Boolean
}
