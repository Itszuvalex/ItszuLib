package com.itszuvalex.itszulib.pathfinding

import com.itszuvalex.itszulib.api.core.Loc4

/**
  * Created by Christopher Harris (Itszuvalex) on 3/1/16.
  */
trait IHeuristicPathfinder extends IPathfinder {

  var getHeuristic: (Loc4) => Float = (_) => 0f

}
