package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.storage.{DynamicIFluidStorageModifiable, IFluidStorage, IFluidStorageModifiable}
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.TileEntityModule
import net.minecraft.util.EnumFacing

class ModuleMultiblockIFluidStorage(val getter: () => Option[IFluidStorageModifiable]) extends TileEntityModule[IFluidStorage] {
  val storage: IFluidStorageModifiable = new DynamicIFluidStorageModifiable(() => getter().getOrElse(IFluidStorage.Empty.asInstanceOf[IFluidStorageModifiable]))

  override def module: IModule[IFluidStorage] = ItszuLibModules.FLUID_STORAGE

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[IFluidStorage] = {
    case null => Some(storage)
    case f =>
      tile.moduleOption(ItszuLibModules.FLUID_STORAGE_CONFIGURABLE, null).map(io => Option(io.getStorageForGlobalFacing(f))).getOrElse(Option(storage))
  }
}
