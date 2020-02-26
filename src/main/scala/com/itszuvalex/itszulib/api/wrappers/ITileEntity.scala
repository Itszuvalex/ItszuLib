package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos

trait ITileEntity {

  def toMinecraft: TileEntity

  def getPos: BlockPos

  def getWorld: IWorld
}
