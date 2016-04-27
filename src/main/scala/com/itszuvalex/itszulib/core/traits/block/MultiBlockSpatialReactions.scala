package com.itszuvalex.itszulib.core.traits.block

import com.itszuvalex.itszulib.api.multiblock.IMultiBlockComponent
import net.minecraft.util.BlockPos
import net.minecraft.world.World

/**
  * Created by Itszuvalex on 1/1/15.
  */
trait MultiBlockSpatialReactions extends MultiBlock with SpatialReactions {
  override def onPickup(world: World, pos: BlockPos): Unit = {
    world.getTileEntity(pos) match {
      case m: IMultiBlockComponent if m.getInfo.isValidMultiBlock => getMultiBlock.breakMultiBlock(world, m.getInfo.cLoc)
      case _ =>
    }
  }

  override def onPlacement(world: World, pos: BlockPos): Unit = {
    getMultiBlock.formMultiBlockWithBlock(world, pos)
  }
}
