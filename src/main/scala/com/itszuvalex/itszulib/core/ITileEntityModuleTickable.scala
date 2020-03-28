package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers.ITileEntity

trait ITileEntityModuleTickable[T] extends ITileEntityModule[T] {

  def clientUpdate(tile: ITileEntity): Unit

  def serverUpdate(tile: ITileEntity): Unit

}
