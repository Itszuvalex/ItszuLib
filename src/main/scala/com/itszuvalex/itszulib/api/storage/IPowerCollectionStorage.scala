package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IPowerAccess, IPowerCollectionAccess}
import com.itszuvalex.itszulib.api.wrappers.IBattery

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
trait IPowerCollectionStorage extends ICollectionStorage[IPowerCollectionStorage, IPowerCollectionAccess, IPowerAccess, IBattery]
