package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers.ITileEntity

abstract class TileEntityModuleTickable[T] extends TileEntityModule[T] with ITileEntityModuleTickable[T] {
  override def clientUpdate(tile: ITileEntity): Unit = {}

  override def serverUpdate(tile: ITileEntity): Unit = {}
}
