package com.itszuvalex.itszulib.pathfinding

import com.itszuvalex.itszulib.api.core.Loc4

import scala.collection.mutable

/**
  * Created by Christopher Harris (Itszuvalex) on 3/1/16.
  */

abstract class SimplePathfinder[B <: mutable.AbstractSeq[Loc4]](_openSet: B) extends IRealTimePathfinder {
  protected val path           = mutable.ArrayBuffer[Loc4]()
  protected val closedSet      = mutable.HashSet[Loc4]()
  protected val originMap      = mutable.HashMap[Loc4, Loc4]()
  protected val openSet: B = _openSet
  protected var completed      = false
  protected var maxPathLength  = Int.MaxValue
  protected var startLoc: Loc4 = null

  override def setMaxPathLength(length: Int) = maxPathLength = length

  def openSetClear()

  def openSetPush(loc: Loc4)

  def openSetPop(): Loc4

  override def initialize(start: Loc4, goalSet: (Loc4) => Boolean): Unit = {
    super.initialize(start, goalSet)

    path.clear()
    closedSet.clear()
    originMap.clear()
    openSetClear()
    startLoc = start
    openSetPush(startLoc)
  }

  override def run(maxExpands: Int): Unit = {
    if (isGoalState == null) throw new IllegalStateException("Cannot search without a goal state.")

    var expands = 0
    while (!isCompleted && openSet.nonEmpty && expands < maxExpands) {
      openSetPop() match {
        case goal if isGoalState(goal) =>
          complete()
          calculatePath(goal)
        case discovered if closedSet.contains(discovered) => /* do nothing */
        case unpathable if !isPathable(unpathable) => closedSet.add(unpathable)
        case expand =>
          getNeighbors(expand).view.filterNot(openSet.contains).filterNot(closedSet.contains).foreach { n =>
            originMap(n) = expand
            openSetPush(n)
                                                                                                      }
          closedSet.add(expand)
      }

      expands += 1
    }
  }

  override def complete(): Unit = {
    completed = true
  }

  override def isCompleted = completed

  /**
    * Due to depth first search, we track the location that was expanded from.  Thus, we reconstruct backwards from destination to source.
    *
    * @param loc Destination location
    */
  def calculatePath(loc: Loc4): Unit = {
    path.clear()

    if (loc == null) {
      return
    }

    var cur = loc
    val next = originMap.get(cur)
    while (next.orNull != cur || cur != startLoc) {
      path.prepend(cur)
      cur = next.orNull
    }
  }

  override def getPath: Seq[Loc4] = path
}
