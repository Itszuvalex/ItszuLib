package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos

class WrapperTileEntity(private val entity: TileEntity) extends ITileEntity {

  override def toMinecraft: TileEntity = entity

  override def getPos: BlockPos = entity.getPos

  override def getWorld: IWorld = new WrapperWorld(entity.getWorld)
}
