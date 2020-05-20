package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.storage.IBattery
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.TileEntityModule
import net.minecraft.util.EnumFacing

abstract class ModuleMultiblockIBattery[N](val getter: () => Option[IBattery[N]]) extends TileEntityModule[IBattery[N]] {

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[IBattery[N]] = _ => getter()
}
