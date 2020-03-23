package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import net.minecraft.util.EnumFacing

abstract class TileEntityInternalModule[T <: TileEntityInternalModule[T]] extends TileEntityModule[T] {
  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[T] = _ => Some(this.asInstanceOf[T])
}
