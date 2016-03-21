package com.itszuvalex.itszulib.pathfinding

import com.itszuvalex.itszulib.api.core.Loc4

import scala.collection._

/**
  * Created by Christopher Harris (Itszuvalex) on 3/1/16.
  */
class DepthFirstPathfinder extends SimplePathfinder[mutable.Stack[Loc4]](mutable.Stack[Loc4]()) {
  override def openSetClear(): Unit = openSet.clear()

  override def openSetPush(loc: Loc4): Unit = openSet.push(loc)

  override def openSetPop(): Loc4 = openSet.pop()
}
