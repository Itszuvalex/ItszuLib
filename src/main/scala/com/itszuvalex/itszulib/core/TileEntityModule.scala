package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers.{IBlock, IItemStack, ITileEntity, IWorld}
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, EnumHand}

abstract class TileEntityModule[T] extends ITileEntityModule[T] {
  override def hasItemNBT: Boolean = false

  override def hasDescriptionNBT: Boolean = false

  override def hasWorldNBT: Boolean = false

  override def writeItemNBT(tag: NBTTagCompound): Unit = {}

  override def readItemNBT(tag: NBTTagCompound): Unit = {}

  override def writeDescriptionNBT(tag: NBTTagCompound): Unit = {}

  override def readDescriptionNBT(tag: NBTTagCompound): Unit = {}

  override def writeWorldNBT(tag: NBTTagCompound): Unit = {}

  override def readWorldNBT(tagCompound: NBTTagCompound): Unit = {}

  override def invalidate(tile: ITileEntity): Unit = {}

  override def validate(tile: ITileEntity): Unit = {}

  override def onChunkUnload(tile: ITileEntity): Unit = {}

  override def onLoad(tile: ITileEntity): Unit = {}

  override def onBlockBreak(core: ITileEntity, state: IBlockState): Unit = {}

  override def onBlockPlacedBy(iworld: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, istack: IItemStack): Unit = {}

  override def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, box: Option[KeyedBoundingBox]): Boolean = false

  override def onNeighborChanged(world: IWorld, pos: BlockPos, state: IBlockState, changedBlock: IBlock, changedPos: BlockPos): Unit = {}
}
