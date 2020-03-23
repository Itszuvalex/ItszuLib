package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.core.{BlockIdentifier, IModule}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.capabilities.Capability

trait ITileEntity {

  def toMinecraft: TileEntity

  def getPos: BlockPos

  def getIWorld: IWorld

  def getBlock: IBlock

  def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean

  def getCapability[T](capability: Capability[T], facing: EnumFacing): T

  def capabilityOption[T](capability: Capability[T], facing: EnumFacing): Option[T] = if (hasCapability(capability, facing)) Option(getCapability(capability, facing)) else None

  def hasModule(mod: IModule[_], facing: EnumFacing): Boolean

  def getModule[T](mod: IModule[T], facing: EnumFacing): T

  def moduleOption[T](mod: IModule[T], facing: EnumFacing): Option[T] = if (hasModule(mod, facing)) Option(getModule(mod, facing)) else None

  def getBlockIdentifier: BlockIdentifier

  def markDirtyForSave(): Unit

  def hasIWorld: Boolean

  def setRenderUpdate(): Unit = if (getIWorld != null) getIWorld.markBlockRangeForRenderUpdate(getPos.getX, getPos.getY, getPos.getZ, getPos.getX, getPos.getY, getPos.getZ)

  def setUpdate(): Unit = if (getIWorld != null) {
    getIWorld.notifyBlockUpdate(getPos, getIWorld.getBlockState(getPos), getIWorld.getBlockState(getPos), 3)
  }

  def notifyNeighborsOfChange(): Unit = if (getIWorld != null) getIWorld.notifyNeighborsOfStateChange(getPos, getBlock, true)

  def isInvalid: Boolean
}
