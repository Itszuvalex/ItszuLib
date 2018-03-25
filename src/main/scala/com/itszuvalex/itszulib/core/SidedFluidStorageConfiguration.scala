package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.storage.IFluidStorage
import net.minecraft.util.EnumFacing

class SidedFluidStorageConfiguration(defaults: (EnumFacing) => String, storages: Map[String, IFluidStorage], front: () => EnumFacing) extends SidedStorageConfiguration[IFluidStorage](defaults, storages, front)
