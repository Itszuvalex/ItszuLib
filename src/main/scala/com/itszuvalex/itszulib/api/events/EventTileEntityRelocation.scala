package com.itszuvalex.itszulib.api.events

import com.itszuvalex.itszulib.api.wrappers.IWorld
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.eventhandler.{Cancelable, Event}

import scala.beans.BeanProperty

/**
  * Created by Itszuvalex on 1/1/15.
  */
object EventTileEntityRelocation {

  /**
    * Posted when a block or item using the SpatialRelocation format will be picked up from the world.
    *
    * @param world
    * @param pos
    */
  @Cancelable
  class Pickup(world: IWorld, pos: BlockPos) extends EventTileEntityRelocation(world, pos: BlockPos)

  /**
    * Posted when a block or item using the SpatialRelocation format will place the given block at the given world coordinates.
    *
    * @param world
    * @param pos
    * @param state
    */
  @Cancelable
  class Placement(world: IWorld, pos: BlockPos, @BeanProperty val state: IBlockState)
    extends EventTileEntityRelocation(world, pos)

}

/**
  * Base class for all SpatialRelocation events.
  *
  * @param world
  * @param pos
  */
@Cancelable abstract class EventTileEntityRelocation(@BeanProperty val world: IWorld, @BeanProperty val pos: BlockPos) extends Event
