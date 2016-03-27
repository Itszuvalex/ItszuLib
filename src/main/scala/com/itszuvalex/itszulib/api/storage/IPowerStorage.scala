package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IPowerCollectionAccess, Revisioned}
import com.itszuvalex.itszulib.api.core.NBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
trait IPowerStorage extends NBTSerializable with Revisioned {

  def getAccess: IPowerCollectionAccess

}
