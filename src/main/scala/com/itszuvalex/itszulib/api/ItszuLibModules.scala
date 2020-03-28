package com.itszuvalex.itszulib.api

import com.itszuvalex.itszulib.api.client.IPreviewable
import com.itszuvalex.itszulib.api.core.{IModule, Module}
import com.itszuvalex.itszulib.api.multiblock.MultiBlockInfo
import com.itszuvalex.itszulib.api.storage.{IFluidStorage, IItemStorage}
import com.itszuvalex.itszulib.core.{SidedFluidStorageConfiguration, SidedItemStorageConfiguration}
import com.itszuvalex.itszulib.util.Color
import net.minecraftforge.fluids.capability.{CapabilityFluidHandler, IFluidHandler}
import net.minecraftforge.items.{CapabilityItemHandler, IItemHandler}

object ItszuLibModules {
  val ITEM_STORAGE              : IModule[IItemStorage]
  = Module.registerModule("ItemStorage", () => ItszuLibCapabilities.ITEM_STORAGE)
  val ITEM_MINECRAFT_INVENTORY  : IModule[IItemHandler]
  = Module.registerModule("ItemMinecraftInventory", () => CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
  val ITEM_PREVIEWABLE          : IModule[IPreviewable]
  = Module.registerModule("ItemPreviewable", () => ItszuLibCapabilities.ITEM_PREVIEWABLE)
  val ITEM_STORAGE_CONFIGURABLE : IModule[SidedItemStorageConfiguration]
  = Module.registerModule("ItemStorageConfigurable", () => ItszuLibCapabilities.ITEM_STORAGE_CONFIGURABLE)
  val FLUID_STORAGE_CONFIGURABLE: IModule[SidedFluidStorageConfiguration]
  = Module.registerModule("FluidStorageConfigurable", () => ItszuLibCapabilities.FLUID_STORAGE_CONFIGURABLE)
  val FLUID_STORAGE             : IModule[IFluidStorage]
  = Module.registerModule("FluidStorage", () => ItszuLibCapabilities.FLUID_STORAGE)
  val FLUID_MINECRAFT_HANDLER   : IModule[IFluidHandler]
  = Module.registerModule("FluidMinecraftHandler", () => CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
  val TILE_MULTIBLOCK           : IModule[MultiBlockInfo]
  = Module.registerModule("TileMultiblock", () => ItszuLibCapabilities.TILE_MULTIBLOCK)
  val COLORABLE                 : IModule[Color]
  = Module.registerModule("Colorable", () => ItszuLibCapabilities.COLORABLE)
}
