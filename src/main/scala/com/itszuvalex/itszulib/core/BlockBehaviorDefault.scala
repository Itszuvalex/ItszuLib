package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers.{IItemStack, IWorld}
import net.minecraft.block.Block
import net.minecraft.block.state.{BlockStateContainer, IBlockState}
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{Mirror, Rotation}

class BlockBehaviorDefault extends IBlockBehavior {
  override def onBlockPlacedBy(iworld: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, istack: IItemStack): Unit = {}

  override def onBlockAdded(world: IWorld, pos: BlockPos, state: IBlockState): Unit = {}

  override def getStateFromMeta(inState: IBlockState, meta: Int): IBlockState = inState

  override def getMetaFromState(inMeta: Int, state: IBlockState): Int = inMeta

  override def withRotation(instate: IBlockState, rot: Rotation): IBlockState = instate

  override def withMirror(instate: IBlockState, mirrorIn: Mirror): IBlockState = instate

  override def createBlockState(block: Block): BlockStateContainer = null
}
