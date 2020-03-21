package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.BlockPos

class WrapperBlock(val block: Block) extends IBlock {
  override def breakBlock(world: IWorld, pos: BlockPos, state: IBlockState): Unit
  = block.breakBlock(Converter.WorldFromIWorld(world), pos, state)

  override def onBlockAdded(world: IWorld, pos: BlockPos, state: IBlockState): Unit
  = block.onBlockAdded(Converter.WorldFromIWorld(world), pos, state)

  override def onBlockPlacedBy(world: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: IItemStack): Unit
  = block.onBlockPlacedBy(Converter.WorldFromIWorld(world), pos, state, placer, Converter.ItemStackFromIItemStack(stack))

  override def toMinecraft: Block = block
}
