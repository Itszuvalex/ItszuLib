package com.itszuvalex.itszulib.core

import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.{BlockPos, EnumFacing}
import net.minecraft.world.World
;

abstract class TileContainer(material: Material) extends BlockContainer(material) {
  setHardness(3f)
  setResistance(3f)

  override def onBlockActivated(worldIn: World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    worldIn.getTileEntity(pos) match {
      case null =>
      case base: TileEntityBase =>
        if (base.canPlayerUse(playerIn)) {
          return base.onSideActivate(playerIn, side)
        }
      case _ =>
    }
    super.onBlockActivated(worldIn, pos, state, playerIn, side, hitX, hitY, hitZ)
  }

  override def breakBlock(world: World, pos: BlockPos, state: IBlockState): Unit = {
    world.getTileEntity(pos) match {
      case null =>
      case tile: TileEntityBase => tile.onBlockBreak()
      case _ =>
    }
    super.breakBlock(world, pos, state)
  }
}
