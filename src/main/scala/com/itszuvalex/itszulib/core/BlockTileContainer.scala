package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers._
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

  override def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, box: Option[KeyedBoundingBox]): Boolean = {
    world.getITileEntity(pos) match {
      case null => false
      case tile: IBlockCallbacks => tile.onBlockActivated(world, pos, state, playerIn, hand, facing, hitX, hitY, hitZ, box)
      case _ => false
    }
  }

  /**
    * There are three of these.
    * 1.  neighborChanged - Deprecated.
    * 2.  observedNeighborChanged - Looks like it's called in the same situations as neighborChanged.  Not deprecated.
    * 3.  onNeighborChanged - Tried to hook this for events, only called when neighboring Tiles were removed.  Not placed.  So wires were broken.
    */
  override def observedNeighborChange(observerState: IBlockState, world: IWorld, observerPos: BlockPos, changedBlock: IBlock, changedBlockPos: BlockPos): Unit = {
    world.getITileEntity(observerPos) match {
      case null =>
      case tile: IBlockCallbacks => tile.onNeighborChanged(world, observerPos, observerState, changedBlock, changedBlockPos)
      case _ =>
    }
  }
}
