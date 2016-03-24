package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IFluidAccess, Revisioned}
import com.itszuvalex.itszulib.api.core.NBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
trait IFluidStorage extends NBTSerializable with Revisioned {

  def getAccess: IFluidAccess

}
