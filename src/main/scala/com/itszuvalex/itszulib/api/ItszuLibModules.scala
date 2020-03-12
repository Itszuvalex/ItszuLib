package com.itszuvalex.itszulib.api

import com.itszuvalex.itszulib.api.core.Module

object ItszuLibModules {
  val ITEM_STORAGE     = new Module(() => ItszuLibCapabilities.ITEM_STORAGE)
  val ITEM_PREVIEWABLE = new Module(() => ItszuLibCapabilities.ITEM_PREVIEWABLE)
  val FLUID_STORAGE    = new Module(() => ItszuLibCapabilities.FLUID_STORAGE)
  val TILE_MULTIBLOCK  = new Module(() => ItszuLibCapabilities.TILE_MULTIBLOCK)
  val COLORABLE        = new Module(() => ItszuLibCapabilities.COLORABLE)
}
