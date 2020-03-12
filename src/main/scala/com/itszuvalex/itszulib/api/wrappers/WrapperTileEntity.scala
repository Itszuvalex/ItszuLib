package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.core.Module
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

  override def hasModule(mod: Module[_], facing: EnumFacing): Boolean = if (mod.hasCapability) {
    hasCapability(mod.capability, facing)
  } else false

  override def getModule[T](mod: Module[T], facing: EnumFacing): T = if (mod.hasCapability) {
    getCapability(mod.capability, facing)
  } else null.asInstanceOf[T]
}
