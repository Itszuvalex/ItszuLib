package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers._
import net.minecraft.block.material.Material
import net.minecraft.block.state.{BlockStateContainer, IBlockState}
import net.minecraft.block.{Block, BlockContainer}
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, EnumHand, Mirror, Rotation}
import net.minecraft.world.World

abstract class TileBlockContainerCore(material: Material, val ibtcDelegate: IBlockTileContainer, val behavior: IBlockBehavior) extends BlockContainer(material) with IBlockTileContainer {

  override def toMinecraft: Block = this

  override def createNewTileEntity(worldIn: World, meta: Int): TileEntity = Converter.TileEntityFromITileEntity(createTileEntity(Converter.IWorldFromWorld(worldIn), meta))

  override def createTileEntity(world: IWorld, meta: Int): ITileEntity = ibtcDelegate.createTileEntity(world, meta)

  override def breakBlock(world: World, pos: BlockPos, state: IBlockState): Unit = {
    breakBlock(Converter.IWorldFromWorld(world), pos, state);
    super.breakBlock(world, pos, state)
  }

  override def breakBlock(world: IWorld, pos: BlockPos, state: IBlockState): Unit = ibtcDelegate.onBlockAdded(world, pos, state)

  override def onBlockAdded(worldIn: World, pos: BlockPos, state: IBlockState): Unit = {
    onBlockAdded(Converter.IWorldFromWorld(worldIn), pos, state)
    super.onBlockAdded(worldIn, pos, state)
  }

  override def onBlockAdded(world: IWorld, pos: BlockPos, state: IBlockState): Unit = {
    behavior.onBlockAdded(world, pos, state)
    ibtcDelegate.onBlockAdded(world, pos, state)
  }

  override def onBlockPlacedBy(worldIn: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack): Unit = {
    onBlockPlacedBy(Converter.IWorldFromWorld(worldIn), pos, state, placer, Converter.IItemStackFromItemStack(stack))
    super.onBlockPlacedBy(worldIn, pos, state, placer, stack)
  }

  override def onBlockPlacedBy(world: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: IItemStack): Unit = {
    behavior.onBlockPlacedBy(world, pos, state, placer, stack)
    ibtcDelegate.onBlockPlacedBy(world, pos, state, placer, stack)
  }


  override def onBlockActivated(worldIn: World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (onBlockActivated(Converter.IWorldFromWorld(worldIn), pos, state, playerIn, hand, facing, hitX, hitY, hitZ))
      true
    else
      super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)
  }

  override def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    ibtcDelegate.onBlockActivated(world, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)
  }

  override def getStateFromMeta(meta: Int): IBlockState = {
    val state = super.getStateFromMeta(meta)
    behavior.getStateFromMeta(state, meta)
  }

  override def getMetaFromState(state: IBlockState): Int = {
    behavior.getMetaFromState(0, state)
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
