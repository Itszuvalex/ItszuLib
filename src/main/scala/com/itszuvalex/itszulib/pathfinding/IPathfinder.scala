package com.itszuvalex.itszulib.pathfinding

import com.itszuvalex.itszulib.api.core.Loc4
import net.minecraftforge.common.util.ForgeDirection

/**
  * Created by Christopher Harris (Itszuvalex) on 3/1/16.
  */
trait IPathfinder {

  var isGoalState: (Loc4) => Boolean = null
  var isPathable: (Loc4) => Boolean = (loc) => loc.getBlock(force = false).exists(_.isBlockSolid(loc.getWorld.orNull, loc.x, loc.y, loc.z, ForgeDirection.UP.ordinal()))
  var getNeighbors: (Loc4) => Set[Loc4] = (loc) => ForgeDirection.VALID_DIRECTIONS.map(loc.getOffset(_, 1)).toSet

  def isCompleted: Boolean

  def complete(): Unit

  def getPath: Seq[Loc4]

  def initialize(start: Loc4, goalLoc: Loc4): Unit = initialize(start, (loc) => loc == goalLoc)

  def initialize(start: Loc4, goalSet: (Loc4) => Boolean): Unit = {
    if (start == null) throw new IllegalArgumentException("Start location cannot be null.")
    if (goalSet == null) throw new IllegalArgumentException("Goal set cannot be empty.")
    isGoalState = goalSet
  }

  def setMaxPathLength(length: Int): Unit

  def run(): Unit

}
