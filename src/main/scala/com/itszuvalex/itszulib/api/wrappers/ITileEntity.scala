package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.capabilities.Capability

trait ITileEntity {

  def toMinecraft: TileEntity

  def getPos: BlockPos

  def getWorld: IWorld

  def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean

  def getCapability[T](capability: Capability[T], facing: EnumFacing): T
}
