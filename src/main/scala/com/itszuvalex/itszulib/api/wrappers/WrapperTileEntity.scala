package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.capabilities.Capability

class WrapperTileEntity(private val entity: TileEntity) extends ITileEntity {

  override def toMinecraft: TileEntity = entity

  override def getPos: BlockPos = entity.getPos

  override def getWorld: IWorld = new WrapperWorld(entity.getWorld)

  override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = entity.hasCapability(capability, facing)

  override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = entity.getCapability(capability, facing)
}
