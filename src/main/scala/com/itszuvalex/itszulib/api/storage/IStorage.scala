package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.IAccess
import com.itszuvalex.itszulib.api.core.NBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/16.
  */
trait IStorage[T <: IStorage[T, A, D], A <: IAccess[A, D], D] extends NBTSerializable {

  def getAccess: A

  def onChanged(): Unit = {}
}
