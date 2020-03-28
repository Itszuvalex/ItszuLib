package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.storage.{DynamicIBattery, IBattery}
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.TileEntityModule
import net.minecraft.util.EnumFacing

abstract class ModuleMultiblockIBattery(val getter: () => Option[IBattery]) extends TileEntityModule[IBattery] {
  val battery: IBattery = new DynamicIBattery(() => getter().getOrElse(IBattery.Empty))

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[IBattery] = _ => getter()
}
