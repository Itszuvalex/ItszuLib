package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers.{Converter, IBlockBehavior, IBlockCallbacks, IBlockTileContainer, IItemStack, IWorld}
import net.minecraft.block.material.Material
import net.minecraft.block.state.{BlockStateContainer, IBlockState}
import net.minecraft.block.{Block, BlockContainer}
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{Mirror, Rotation}
import net.minecraft.world.World

abstract class TileBlockContainerCore(material: Material, behavior: IBlockBehavior) extends BlockContainer(material) with IBlockTileContainer {

  override def toMinecraft: Block = this

  override def createNewTileEntity(worldIn: World, meta: Int): TileEntity = Converter.TileEntityFromITileEntity(createTileEntity(Converter.IWorldFromWorld(worldIn), meta))

  override def breakBlock(world: World, pos: BlockPos, state: IBlockState): Unit = {
    breakBlock(Converter.IWorldFromWorld(world), pos, state);
    super.breakBlock(world, pos, state)
  }

  override def breakBlock(world: IWorld, pos: BlockPos, state: IBlockState): Unit = {
    world.getTileEntity(pos) match {
      case null =>
      case tile: IBlockCallbacks => tile.onBlockBreak(state)
      case _ =>
    }
  }

  override def onBlockAdded(worldIn: World, pos: BlockPos, state: IBlockState): Unit = {
    onBlockAdded(Converter.IWorldFromWorld(worldIn), pos, state)
    super.onBlockAdded(worldIn, pos, state)
  }

  override def onBlockAdded(world: IWorld, pos: BlockPos, state: IBlockState): Unit = {
    behavior.onBlockAdded(world, pos, state)
  }

  override def onBlockPlacedBy(worldIn: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack): Unit = {
    onBlockPlacedBy(Converter.IWorldFromWorld(worldIn), pos, state, placer, Converter.IItemStackFromItemStack(stack))
    super.onBlockPlacedBy(worldIn, pos, state, placer, stack)
  }

  override def onBlockPlacedBy(world: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: IItemStack): Unit = {
    behavior.onBlockPlacedBy(world, pos, state, placer, stack)
    world.getTileEntity(pos) match {
      case null =>
      case tile: IBlockCallbacks => tile.onBlockPlacedBy(world, pos, state, placer, stack)
      case _ =>
    }
  }

  override def getStateFromMeta(meta: Int): IBlockState = {
    val state = super.getStateFromMeta(meta)
    behavior.getStateFromMeta(state, meta)
  }

  override def getMetaFromState(state: IBlockState): Int = {
    val meta = super.getMetaFromState(state)
    behavior.getMetaFromState(meta, state)
  }

  override def withRotation(state: IBlockState, rot: Rotation): IBlockState = {
    val instate = super.withRotation(state, rot)
    behavior.withRotation(instate, rot)
  }

  override def withMirror(state: IBlockState, mirrorIn: Mirror): IBlockState = {
    val instate = super.withMirror(state, mirrorIn)
    behavior.withMirror(instate, mirrorIn)
  }

  override def createBlockState(): BlockStateContainer = {
    val container: BlockStateContainer = behavior.createBlockState(this)
    if (container != null)
      container
    else
      super.createBlockState()
  }
}
