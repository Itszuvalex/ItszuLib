package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers.{IBlockCallbacks, IBlockTileContainer, IItemStack, IWorld}
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, EnumHand}

abstract class BlockTileContainer(private val block: () => Block) extends IBlockTileContainer {
  override def toMinecraft: Block = block()

  override def breakBlock(world: IWorld, pos: BlockPos, state: IBlockState): Unit =
    world.getITileEntity(pos) match {
      case null =>
      case tile: IBlockCallbacks => tile.onBlockBreak(state)
      case _ =>
    }

  override def onBlockAdded(world: IWorld, pos: BlockPos, state: IBlockState): Unit = {}

  override def onBlockPlacedBy(world: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: IItemStack): Unit =
    world.getITileEntity(pos) match {
      case null =>
      case tile: IBlockCallbacks => tile.onBlockPlacedBy(world, pos, state, placer, stack)
      case _ =>
    }

  override def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    world.getITileEntity(pos) match {
      case null => false
      case tile: IBlockCallbacks => tile.onBlockActivated(world, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)
      case _ => false
    }
  }
}
