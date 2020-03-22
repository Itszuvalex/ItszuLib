package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.wrappers.{Converter, ITileEntity}
import com.itszuvalex.itszulib.core.TileEntityModule
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.capability.IFluidHandler

class ModuleIFluidHandlerConverter extends TileEntityModule[IFluidHandler] {
  override def module: IModule[IFluidHandler] = ItszuLibModules.FLUID_MINECRAFT_HANDLER

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[IFluidHandler] = {
    case null => tile.moduleOption(ItszuLibModules.FLUID_STORAGE, null)
                     .map(Converter.IFluidHandlerFromIFluidStorage)
    case f =>
      tile.moduleOption(ItszuLibModules.FLUID_STORAGE_CONFIGURABLE, null) match {
        case Some(io) =>
          Option(io.getStorageForGlobalFacing(f)).map(Converter.IFluidHandlerFromIFluidStorage)
        case None => tile.moduleOption(ItszuLibModules.FLUID_STORAGE, null).map(Converter.IFluidHandlerFromIFluidStorage)
      }
  }

}
