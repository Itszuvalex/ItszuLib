package com.itszuvalex.itszulib.core.traits.block

import net.minecraft.util.BlockPos
import net.minecraft.world.World

/**
  * Created by Itszuvalex on 1/1/15.
  */
trait SpatialReactions {

  def onPickup(world: World, pos: BlockPos)

  def onPlacement(world: World, pos: BlockPos)

}
