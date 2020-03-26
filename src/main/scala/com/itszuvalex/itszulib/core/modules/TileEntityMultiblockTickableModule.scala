package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.multiblock.MultiBlockInfo
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.TileEntityInternalModuleTickable

abstract class TileEntityMultiblockTickableModule[T <: TileEntityMultiblockTickableModule[T]](val info: MultiBlockInfo) extends TileEntityInternalModuleTickable[T] {
  override def clientUpdate(tile: ITileEntity): Unit = {
    if (info.isValidMultiBlock && info.isController)
      clientControllerUpdate(tile)
  }

  override def serverUpdate(tile: ITileEntity): Unit = {
    if (info.isValidMultiBlock && info.isController)
      serverControllerUpdate(tile)
  }

  def clientControllerUpdate(tile: ITileEntity): Unit = {}

  def serverControllerUpdate(tile: ITileEntity): Unit = {}
}
