package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.core.KeyedBoundingBox
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, EnumHand}

trait IBlock {

  def toMinecraft: Block

  def breakBlock(world: IWorld, pos: BlockPos, state: IBlockState): Unit

  def onBlockAdded(world: IWorld, pos: BlockPos, state: IBlockState): Unit

  def onBlockPlacedBy(world: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: IItemStack): Unit

  def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, box: Option[KeyedBoundingBox]): Boolean

  /**
    * There are three of these.
    * 1.  neighborChanged - Deprecated.
    * 2.  observedNeighborChanged - Looks like it's called in the same situations as neighborChanged.  Not deprecated.
    * 3.  onNeighborChanged - Tried to hook this for events, only called when neighboring Tiles were removed.  Not placed.  So wires were broken.
    */
  def observedNeighborChange(observerState: IBlockState, world: IWorld, observerPos: BlockPos, changedBlock: IBlock, changedBlockPos: BlockPos): Unit

}
