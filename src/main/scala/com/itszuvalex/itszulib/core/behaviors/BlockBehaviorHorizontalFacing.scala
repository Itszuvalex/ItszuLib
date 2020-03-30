package com.itszuvalex.itszulib.core.behaviors

import com.itszuvalex.itszulib.api.wrappers.{IBlock, IBlockBehavior, IItemStack, IWorld}
import com.itszuvalex.itszulib.core.behaviors.BlockBehaviorHorizontalFacing.FACING
import net.minecraft.block.state.{BlockStateContainer, IBlockState}
import net.minecraft.block.{Block, BlockHorizontal}
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, Mirror, Rotation}

object BlockBehaviorHorizontalFacing {
  final val FACING = BlockHorizontal.FACING
}

private class BlockBehaviorHorizontalFacing extends IBlockBehavior {

  override def breakBlock(world: IWorld, pos: BlockPos, state: IBlockState): Unit = {}

  override def onBlockPlacedBy(iworld: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, istack: IItemStack): Unit = {
    iworld.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing.getOpposite), 2)
  }

  override def onBlockAdded(world: IWorld, pos: BlockPos, state: IBlockState): Unit = {
    setDefaultFacing(world, pos, state)
  }

  override def observedNeighborChange(observerState: IBlockState, world: IWorld, observerPos: BlockPos, changedBlock: IBlock, changedBlockPos: BlockPos): Unit = {}

  override def getStateFromMeta(inState: IBlockState, meta: Int): IBlockState = {
    var enumfacing: EnumFacing = EnumFacing.getFront(meta)
    if (enumfacing.getAxis eq EnumFacing.Axis.Y) enumfacing = EnumFacing.NORTH
    inState.withProperty(FACING, enumfacing)
  }

  override def getMetaFromState(inMeta: Int, state: IBlockState): Int = state.getValue(FACING).getIndex

  override def withRotation(instate: IBlockState, rot: Rotation): IBlockState = instate.withProperty(FACING, rot.rotate(instate.getValue(FACING)))

  override def withMirror(instate: IBlockState, mirrorIn: Mirror): IBlockState = instate.withRotation(mirrorIn.toRotation(instate.getValue(FACING)))

  override def createBlockState(block: Block): BlockStateContainer = new BlockStateContainer(block, FACING)

  private def setDefaultFacing(worldIn: IWorld, pos: BlockPos, state: IBlockState) {
    if (!worldIn.isRemote) {
      val iblockstate  = worldIn.getBlockState(pos.north())
      val iblockstate1 = worldIn.getBlockState(pos.south())
      val iblockstate2 = worldIn.getBlockState(pos.west())
      val iblockstate3 = worldIn.getBlockState(pos.east())
      var enumfacing   = state.getValue(FACING)

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

}
