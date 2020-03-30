package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.core.KeyedBoundingBox
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, EnumHand}

trait IBlockCallbacks {
  def onBlockBreak(state: IBlockState): Unit

  def onBlockPlacedBy(iworld: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, istack: IItemStack): Unit

  def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, box: Option[KeyedBoundingBox]): Boolean

  /**
    * Called from observedNeighborChanged, not onNeighborChanged from regular blocks, due to the latter not triggering on NEW tile entities placed next to this block.
    *
    * @param world
    * @param pos
    * @param state
    * @param changedBlock
    * @param changedPos
    */
  def onNeighborChanged(world: IWorld, pos: BlockPos, state: IBlockState, changedBlock: IBlock, changedPos: BlockPos): Unit
}
