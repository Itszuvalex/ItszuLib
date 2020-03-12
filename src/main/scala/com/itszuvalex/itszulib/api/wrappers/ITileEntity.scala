package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.core.Module
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

  def capabilityOption[T](capability: Capability[T], facing: EnumFacing): Option[T] = if (hasCapability(capability, facing)) Option(getCapability(capability, facing)) else None

  def hasModule(mod: Module[_], facing: EnumFacing): Boolean

  def getModule[T](mod: Module[T], facing: EnumFacing): T

  def moduleOption[T](mod: Module[T], facing: EnumFacing): Option[T] = if (hasModule(mod, facing)) Option(getModule(mod, facing)) else None
}
