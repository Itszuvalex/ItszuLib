package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.core.{BlockIdentifier, IModule, Loc4}
import com.itszuvalex.itszulib.api.wrappers.{IBlock, ITileEntity, IWorld}
import com.itszuvalex.itszulib.core.ModuleCapabilityMap
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class TestableNetworkNodeTileEntity(pos: Loc4, world: IWorld, block: IBlock) extends ITileEntity {
  val moduleCapabilityMap = new ModuleCapabilityMap

  override def toMinecraft: TileEntity = null

  override def getPos: BlockPos = pos.getPos

  override def getIWorld: IWorld = world

  override def getBlock: IBlock = null

  override def hasModule(mod: IModule[_], facing: EnumFacing): Boolean = moduleCapabilityMap.hasModule(mod, facing)

  override def getModule[T](mod: IModule[T], facing: EnumFacing): T = moduleCapabilityMap.getModule(mod, facing)

  override def getBlockIdentifier: BlockIdentifier = BlockIdentifier("Test", "TestableTileEntity")

  override def markDirtyForSave(): Unit = {}

  override def hasIWorld: Boolean = true

  override def isInvalid: Boolean = false
}
