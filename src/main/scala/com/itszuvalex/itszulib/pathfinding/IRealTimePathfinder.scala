package com.itszuvalex.itszulib.pathfinding

/**
  * Created by Christopher Harris (Itszuvalex) on 3/1/16.
  */
trait IRealTimePathfinder extends IPathfinder {

  def run(maxExpands: Int): Unit

  override def run() = run(Int.MaxValue)

}
