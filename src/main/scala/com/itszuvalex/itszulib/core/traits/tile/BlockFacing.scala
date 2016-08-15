package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.core.traits.tile.BlockFacing.FACING
import net.minecraft.block.state.{BlockStateContainer, IBlockState}
import net.minecraft.block.{Block, BlockHorizontal}
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, Mirror, Rotation}
import net.minecraft.world.World

/**
  * Created by Chris on 8/14/2016.
  */
object BlockFacing {
  final val FACING = BlockHorizontal.FACING
}

trait BlockFacing extends Block {

  def addFacing(state: IBlockState): IBlockState = {
    state.withProperty(FACING, EnumFacing.NORTH)
    state
  }

  override def onBlockAdded(worldIn: World, pos: BlockPos, state: IBlockState): Unit = {
    super.onBlockAdded(worldIn, pos, state)
    setDefaultFacing(worldIn, pos, state)
  }

  private def setDefaultFacing(worldIn: World, pos: BlockPos, state: IBlockState) {
    if (!worldIn.isRemote) {
      val iblockstate = worldIn.getBlockState(pos.north())
      val iblockstate1 = worldIn.getBlockState(pos.south())
      val iblockstate2 = worldIn.getBlockState(pos.west())
      val iblockstate3 = worldIn.getBlockState(pos.east())
      var enumfacing = state.getValue(FACING)

      if (enumfacing == EnumFacing.NORTH && iblockstate.isFullBlock && !iblockstate1.isFullBlock) {
        enumfacing = EnumFacing.SOUTH
      }
      else if (enumfacing == EnumFacing.SOUTH && iblockstate1.isFullBlock && !iblockstate.isFullBlock) {
        enumfacing = EnumFacing.NORTH
      }
      else if (enumfacing == EnumFacing.WEST && iblockstate2.isFullBlock && !iblockstate3.isFullBlock) {
        enumfacing = EnumFacing.EAST
      }
      else if (enumfacing == EnumFacing.EAST && iblockstate3.isFullBlock && !iblockstate2.isFullBlock) {
        enumfacing = EnumFacing.WEST
      }

      worldIn.setBlockState(pos, state.withProperty(FACING, enumfacing), 2)
    }
  }

  override def onBlockPlacedBy(worldIn: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack): Unit = {
    worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing.getOpposite), 2)
  }

  override def onBlockPlaced(worldIn: World, pos: BlockPos, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, meta: Int, placer: EntityLivingBase): IBlockState = {
    getDefaultState.withProperty(FACING, placer.getHorizontalFacing.getOpposite)
  }

  /**
    * Convert the given metadata into a BlockState for this Block
    */
  override def getStateFromMeta(meta: Int): IBlockState = {
    var enumfacing: EnumFacing = EnumFacing.getFront(meta)
    if (enumfacing.getAxis eq EnumFacing.Axis.Y) enumfacing = EnumFacing.NORTH
    this.getDefaultState.withProperty(FACING, enumfacing)
  }

  /**
    * Convert the BlockState into the correct metadata value
    */
  override def getMetaFromState(state: IBlockState): Int = state.getValue(FACING).getIndex

  /**
    * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed
    * blockstate.
    */
  override def withRotation(state: IBlockState, rot: Rotation): IBlockState = state.withProperty(FACING, rot.rotate(state.getValue(FACING)))

  /**
    * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed
    * blockstate.
    */
  override def withMirror(state: IBlockState, mirrorIn: Mirror): IBlockState = state.withRotation(mirrorIn.toRotation(state.getValue(FACING)))

  override protected def createBlockState: BlockStateContainer = new BlockStateContainer(this, FACING)
}
