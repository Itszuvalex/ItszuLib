package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.core.NBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 3/26/16.
  */
trait IStorage[T] extends NBTSerializable {
  protected var store: T

  def remove(a: T): T

  def add(a: T): T

  def canAdd(a: T): Boolean

  def canRemove(a: T): Boolean

  def getCurrent: T

  def getMax: T

}
