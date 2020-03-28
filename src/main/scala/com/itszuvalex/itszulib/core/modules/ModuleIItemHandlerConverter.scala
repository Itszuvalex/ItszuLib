package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.wrappers.{Converter, ITileEntity}
import com.itszuvalex.itszulib.core.TileEntityModule
import net.minecraft.util.EnumFacing
import net.minecraftforge.items.IItemHandler

class ModuleIItemHandlerConverter extends TileEntityModule[IItemHandler] {
  override def module: IModule[IItemHandler] = ItszuLibModules.ITEM_MINECRAFT_INVENTORY

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[IItemHandler] = {
    case null => tile.moduleOption(ItszuLibModules.ITEM_STORAGE, null)
                     .map(Converter.IItemHandlerModifiableFromIItemStorage)
    case f =>
      tile.moduleOption(ItszuLibModules.ITEM_STORAGE_CONFIGURABLE, null) match {
        case Some(io) =>
          Option(io.getStorageForGlobalFacing(f)).map(Converter.IItemHandlerModifiableFromIItemStorage)
        case None => tile.moduleOption(ItszuLibModules.ITEM_STORAGE, null).map(Converter.IItemHandlerModifiableFromIItemStorage)
      }
  }
}
