package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.storage.{DynamicIItemStorage, IItemStorage}
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.TileEntityModule
import net.minecraft.util.EnumFacing

class ModuleMultiblockIItemStorage(val getter: () => Option[IItemStorage]) extends TileEntityModule[IItemStorage] {
  val storage: IItemStorage = new DynamicIItemStorage(() => getter().getOrElse(IItemStorage.Empty))

  override def module: IModule[IItemStorage] = ItszuLibModules.ITEM_STORAGE

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[IItemStorage] = {
    case null => Some(storage)
    case f =>
      tile.moduleOption(ItszuLibModules.ITEM_STORAGE_CONFIGURABLE, null).map(io => Option(io.getStorageForGlobalFacing(f))).getOrElse(Option(storage))
  }
}
