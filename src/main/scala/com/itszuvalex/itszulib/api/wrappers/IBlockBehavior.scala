package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.block.Block
import net.minecraft.block.state.{BlockStateContainer, IBlockState}
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{Mirror, Rotation}

trait IBlockBehavior {

  def breakBlock(world: IWorld, pos: BlockPos, state: IBlockState): Unit

  def onBlockPlacedBy(iworld: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, istack: IItemStack): Unit

  def onBlockAdded(world: IWorld, pos: BlockPos, state: IBlockState): Unit

  def getStateFromMeta(inState: IBlockState, meta: Int): IBlockState

  def getMetaFromState(inMeta: Int, state: IBlockState): Int

  def withRotation(instate: IBlockState, rot: Rotation): IBlockState

  def withMirror(instate: IBlockState, mirrorIn: Mirror): IBlockState

  def createBlockState(block: Block): BlockStateContainer
}
