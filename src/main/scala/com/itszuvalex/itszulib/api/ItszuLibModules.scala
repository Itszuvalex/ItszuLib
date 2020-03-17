package com.itszuvalex.itszulib.api

import com.itszuvalex.itszulib.api.core.Module

object ItszuLibModules {
  val ITEM_STORAGE     = Module.registerModule("ItemStorage", () => ItszuLibCapabilities.ITEM_STORAGE)
  val ITEM_PREVIEWABLE = Module.registerModule("ItemPreviewable", () => ItszuLibCapabilities.ITEM_PREVIEWABLE)
  val FLUID_STORAGE    = Module.registerModule("FluidStorage", () => ItszuLibCapabilities.FLUID_STORAGE)
  val TILE_MULTIBLOCK  = Module.registerModule("TileMultiblock", () => ItszuLibCapabilities.TILE_MULTIBLOCK)
  val COLORABLE        = Module.registerModule("Colorable", () => ItszuLibCapabilities.COLORABLE)
}
