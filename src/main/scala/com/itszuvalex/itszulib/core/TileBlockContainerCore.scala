package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers.{Converter, IItemStack, IWorld, WrapperWorld}
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.block.state.{BlockStateContainer, IBlockState}
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{Mirror, Rotation}
import net.minecraft.world.World

abstract class TileBlockContainerCore(material: Material, behavior: IBlockBehavior) extends BlockContainer(material) {
  override def createNewTileEntity(worldIn: World, meta: Int): TileEntity = createTileEntity(new WrapperWorld(worldIn), meta)

  def createTileEntity(world: IWorld, meta: Int): TileEntityCore

  override def breakBlock(world: World, pos: BlockPos, state: IBlockState): Unit = {
    world.getTileEntity(pos) match {
      case null =>
      case tile: TileEntityCore => tile.onBlockBreak(state)
      case _ =>
    }
    super.breakBlock(world, pos, state)
  }

  override def onBlockAdded(worldIn: World, pos: BlockPos, state: IBlockState): Unit = {
    behavior.onBlockAdded(new WrapperWorld(worldIn), pos, state)
    super.onBlockAdded(worldIn, pos, state)
  }

  override def onBlockPlacedBy(worldIn: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack): Unit = {
    val iworld: IWorld     = new WrapperWorld(worldIn)
    val istack: IItemStack = Converter.IItemStackFromItemStack(stack)
    behavior.onBlockPlacedBy(iworld, pos, state, placer, istack)
    iworld.getTileEntity(pos) match {
      case null =>
      case tile: TileEntityCore => tile.onBlockPlacedBy(iworld, pos, state, placer, istack)
      case _ =>
    }
    super.onBlockPlacedBy(worldIn, pos, state, placer, stack)
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
