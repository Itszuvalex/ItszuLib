package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IFluidAccess, IFluidCollectionAccess}
import com.itszuvalex.itszulib.api.wrappers.IFluidStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
trait IFluidCollectionStorage extends ICollectionStorage[IFluidCollectionStorage, IFluidCollectionAccess, IFluidAccess, IFluidStack]

