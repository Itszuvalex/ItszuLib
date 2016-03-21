package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.core.TileContainer
import net.minecraft.block.material.Material
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

/**
  * Created by Christopher on 4/11/2015.
  */
class BlockPortalTest extends TileContainer(Material.iron) {
  override def createNewTileEntity(p_149915_1_ : World, p_149915_2_ : Int): TileEntity = new PortalTileTest

  override def isOpaqueCube = false

  override def renderAsNormalBlock = false

  override def getRenderType = -1
}
