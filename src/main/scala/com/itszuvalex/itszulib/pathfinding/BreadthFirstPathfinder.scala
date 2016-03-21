package com.itszuvalex.itszulib.pathfinding

import com.itszuvalex.itszulib.api.core.Loc4

import scala.collection._

/**
  * Created by Christopher Harris (Itszuvalex) on 3/1/16.
  */
class BreadthFirstPathfinder extends SimplePathfinder[mutable.Queue[Loc4]](mutable.Queue[Loc4]()) {
  override def openSetClear(): Unit = openSet.clear()

  override def openSetPush(loc: Loc4): Unit = openSet.enqueue(loc)

  override def openSetPop(): Loc4 = openSet.dequeue()
}
