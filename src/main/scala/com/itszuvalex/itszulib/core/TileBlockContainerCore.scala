package com.itszuvalex.itszulib.core

import java.util

import com.itszuvalex.itszulib.api.wrappers._
import net.minecraft.block.material.Material
import net.minecraft.block.state.{BlockStateContainer, IBlockState}
import net.minecraft.block.{Block, BlockContainer}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.{AxisAlignedBB, BlockPos, RayTraceResult, Vec3d}
import net.minecraft.util.{EnumFacing, EnumHand, Mirror, Rotation}
import net.minecraft.world.{IBlockAccess, World}

object TileBlockContainerCore {
  val DEFAULT_BOUNDING_BOX: KeyedBoundingBox = KeyedBoundingBox("Default", 0, new AxisAlignedBB(0, 0, 0, 1, 1, 1))

  def DEFAULT_STATIC_BOUNDING_BOX_COLLECTION = new NamedStaticBoundingBoxCollection(TileBlockContainerCore.DEFAULT_BOUNDING_BOX _, Array(TileBlockContainerCore.DEFAULT_BOUNDING_BOX))
}

abstract class TileBlockContainerCore(material: Material, val ibtcDelegate: IBlockTileContainer, val behavior: IBlockBehavior) extends BlockContainer(material) with IBlockTileContainer {
  val boundingBoxes: Option[NamedBoundingBoxCollection] = Some(TileBlockContainerCore.DEFAULT_STATIC_BOUNDING_BOX_COLLECTION)

  override def toMinecraft: Block = this

  override def createNewTileEntity(worldIn: World, meta: Int): TileEntity = Converter.TileEntityFromITileEntity(createTileEntity(Converter.IWorldFromWorld(worldIn), meta))

  override def createTileEntity(world: IWorld, meta: Int): ITileEntity = ibtcDelegate.createTileEntity(world, meta)

  override def breakBlock(world: World, pos: BlockPos, state: IBlockState): Unit = {
    breakBlock(Converter.IWorldFromWorld(world), pos, state)
    super.breakBlock(world, pos, state)
  }

  override def breakBlock(world: IWorld, pos: BlockPos, state: IBlockState): Unit = {
    behavior.breakBlock(world, pos, state)
    ibtcDelegate.breakBlock(world, pos, state)
  }

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


  /**
    * There are three of these.
    * 1.  neighborChanged - Deprecated.
    * 2.  observedNeighborChanged - Looks like it's called in the same situations as neighborChanged.  Not deprecated.
    * 3.  onNeighborChanged - Tried to hook this for events, only called when neighboring Tiles were removed.  Not placed.  So wires were broken.
    */
  override def observedNeighborChange(observerState: IBlockState, world: IWorld, observerPos: BlockPos, changedBlock: IBlock, changedBlockPos: BlockPos): Unit = {
    behavior.observedNeighborChange(observerState, world, observerPos, changedBlock, changedBlockPos)
    ibtcDelegate.observedNeighborChange(observerState, world, observerPos, changedBlock, changedBlockPos)
  }

  override def observedNeighborChange(observerState: IBlockState, world: World, observerPos: BlockPos, changedBlock: Block, changedBlockPos: BlockPos): Unit = {
    val iworld = Converter.IWorldFromWorld(world)
    val iblock = Converter.IBlockFromBlock(changedBlock)
    observedNeighborChange(observerState, iworld, observerPos, iblock, changedBlockPos)
  }

  override def onBlockActivated(worldIn: World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    val iworld = Converter.IWorldFromWorld(worldIn)
    boundingBoxes match {
      case None =>
        if (onBlockActivated(iworld, pos, state, playerIn, hand, facing, hitX, hitY, hitZ, None))
          true
        else
          super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)
      case Some(x) =>
        if (onBlockActivated(iworld, pos, state, playerIn, hand, facing, hitX, hitY, hitZ, x.getBoxFromPoint(iworld, pos, state, playerIn, facing, hitX, hitY, hitZ)))
          true
        else
          super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)
    }
  }

  override def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, box: Option[KeyedBoundingBox]): Boolean = {
    ibtcDelegate.onBlockActivated(world, pos, state, playerIn, hand, facing, hitX, hitY, hitZ, box)
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

  override def addCollisionBoxToList(state: IBlockState, worldIn: World, pos: BlockPos, entityBox: AxisAlignedBB, collidingBoxes: util.List[AxisAlignedBB], entityIn: Entity, isActualState: Boolean): Unit = {
    super.addCollisionBoxToList(state, worldIn, pos, entityBox, collidingBoxes, entityIn, isActualState)
    boundingBoxes match {
      case None =>
      case Some(x) =>
        x.getIntersectingBoxes(Converter.IWorldFromWorld(worldIn), pos, entityBox).foreach(collidingBoxes.add)
    }
  }

  override def collisionRayTrace(blockState: IBlockState, worldIn: World, pos: BlockPos, start: Vec3d, end: Vec3d): RayTraceResult = {
    boundingBoxes match {
      case None =>
        super.collisionRayTrace(blockState, worldIn, pos, start, end)
      case Some(x) =>
        x.collisionRayTrace(blockState, Converter.IWorldFromWorld(worldIn), pos, start, end)
    }
  }

  override def getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB = {
    boundingBoxes match {
      case None =>
        super.getBoundingBox(state, source, pos)
      case Some(x) =>
        x.defaultBoundingBox().box
    }
  }

  override def getSelectedBoundingBox(state: IBlockState, worldIn: World, pos: BlockPos): AxisAlignedBB = {
    boundingBoxes match {
      case None =>
        super.getSelectedBoundingBox(state, worldIn, pos)
      case Some(x) =>
        x.getSelectedBoundingBox(state, Converter.IWorldFromWorld(worldIn), pos)
    }
  }
}
