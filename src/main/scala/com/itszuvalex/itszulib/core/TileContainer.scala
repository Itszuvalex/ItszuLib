package com.itszuvalex.itszulib.core

import net.minecraft.block.material.Material
import net.minecraft.block.{Block, BlockContainer}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
;

abstract class TileContainer(material: Material) extends BlockContainer(material) {
  setHardness(3f)
  setResistance(3f)

  override def onBlockActivated(par1World: World, par2: Int, par3: Int, par4: Int, par5EntityPlayer: EntityPlayer,
                                par6: Int, par7: Float, par8: Float, par9: Float): Boolean = {
    par1World.getTileEntity(par2, par3, par4) match {
      case null =>
      case base: TileEntityBase =>
        if (base.canPlayerUse(par5EntityPlayer)) {
          return base.onSideActivate(par5EntityPlayer, par6)
        }
      case _ =>
    }
    super.onBlockActivated(par1World, par2, par3, par4, par5EntityPlayer, par6, par7, par8, par9)
  }

  override def breakBlock(world: World, x: Int, y: Int, z: Int, block: Block, metadata: Int): Unit = {
    world.getTileEntity(x, y, z) match {
      case null =>
      case tile: TileEntityBase => tile.onBlockBreak()
      case _ =>
    }
    super.breakBlock(world, x, y, z, block, metadata)
  }
}
