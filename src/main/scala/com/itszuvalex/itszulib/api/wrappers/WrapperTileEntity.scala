package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.core.{BlockIdentifier, IModule}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class WrapperTileEntity(private val entity: TileEntity) extends ITileEntity {

  override def toMinecraft: TileEntity = entity

  override def getPos: BlockPos = entity.getPos

  override def getIWorld: IWorld = new WrapperWorld(entity.getWorld)

  override def getBlockIdentifier: BlockIdentifier = BlockIdentifier(entity.getBlockType)

  override def markDirtyForSave(): Unit = entity.markDirty()

  override def hasIWorld: Boolean = entity.hasWorld

  override def hasModule(mod: IModule[_], facing: EnumFacing): Boolean = if (mod.hasCapability) {
    entity.hasCapability(mod.capability, facing)
  } else false

  override def getModule[T](mod: IModule[T], facing: EnumFacing): T = if (mod.hasCapability) {
    entity.getCapability(mod.capability, facing)
  } else null.asInstanceOf[T]

  override def getBlock: IBlock = Converter.IBlockFromBlock(entity.getBlockType)

  override def isInvalid: Boolean = entity.isInvalid
}
