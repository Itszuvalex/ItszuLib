package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.storage.IItemStorage
import net.minecraft.util.EnumFacing

/**
  * Created by Christopher Harris (Itszuvalex) on 2/28/17.
  */
class SidedItemStorageConfiguration(defaults: (EnumFacing) => String, storages: Map[String, IItemStorage], front: () => EnumFacing) extends SidedStorageConfiguration[IItemStorage](defaults, storages, front)
