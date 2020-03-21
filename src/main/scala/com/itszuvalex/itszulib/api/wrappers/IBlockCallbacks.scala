package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, EnumHand}

trait IBlockCallbacks {
  def onSideActivate(par5EntityPlayer: EntityPlayer, side: EnumFacing): Boolean

  def onBlockBreak(state: IBlockState): Unit

  def onBlockPlacedBy(iworld: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, istack: IItemStack): Unit

  def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean
}
