package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.wrappers.IBattery

/**
  * Created by Christopher Harris (Itszuvalex) on 3/12/2016.
  */
trait IPowerAccess extends IAccess[IPowerAccess, IBattery] {

  def storage: Option[Double] = get.map(_.storage)

  def maxStorage: Option[Double] = get.map(_.maxStorage)

  /**
    * Copies all required info from another A
    *
    * @param other
    */
  override def copyFromAccess(other: IPowerAccess, deepCopy: Boolean): Unit = {
    set(other.get.map(_.copy()).orNull)
  }

  def charge(amt: Int): Double = get.map { i =>
    if (amt < 0) return 0

    val amount = Math.min(room.get, amt)
    i.storage += amount
    onChanged()
    amount
                                         }.getOrElse(0)

  def room = maxStorage.map(_ - storage.getOrElse(0d))

  def drain(amt: Int): Double = get.map { i =>
    if (amt < 0) return 0

    val amount = Math.min(get.get.storage, amt)
    i.storage -= amount
    if (i.storage <= 0)
      clear()
    onChanged()
    amount
                                        }.getOrElse(0)

  def clear(): Unit = {
    get.foreach(_.storage = 0)
  }

  def isValid: Boolean
}
