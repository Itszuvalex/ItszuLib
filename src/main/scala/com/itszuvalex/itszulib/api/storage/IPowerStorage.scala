package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IPowerAccess, IPowerCollectionAccess}

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
trait IPowerStorage extends IStorage[IPowerStorage, IPowerCollectionAccess, IPowerAccess, Double]
