package com.itszuvalex.itszulib.api.wrappers

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

  def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean

}
