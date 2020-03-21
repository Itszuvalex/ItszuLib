package com.itszuvalex.itszulib.api.wrappers

trait IBlockTileContainer extends IBlock {

  def createTileEntity(world: IWorld, meta: Int): ITileEntity
}
