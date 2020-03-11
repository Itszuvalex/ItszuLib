package com.itszuvalex.itszulib.logistics

import com.google.common.math.LongMath
import com.itszuvalex.itszulib.api.core.{ChunkCoord, Loc4}
import com.itszuvalex.itszulib.logistics.LocationTracker._

import scala.collection.mutable

/**
  * Created by Christopher on 7/29/2015.
  */
object LocationTracker {
  private val CHUNK_SIZE = 16
}


class LocationTracker {
  private val trackerMap = mutable.HashMap[Int, mutable.HashMap[ChunkCoord, mutable.HashSet[Loc4]]]()

  def trackLocation(loc: Loc4) = {
    trackerMap.getOrElseUpdate(loc.dim, mutable.HashMap[ChunkCoord, mutable.HashSet[Loc4]]()).getOrElseUpdate(loc.chunkCoords, mutable.HashSet[Loc4]()) += loc
  }

  def removeLocation(loc: Loc4) = {
    trackerMap.get(loc.dim) match {
      case None =>
      case Some(dim) =>
        dim.get(loc.chunkCoords) match {
          case None =>
          case Some(chunk) =>
            chunk -= loc
            if (chunk.isEmpty) dim -= loc.chunkCoords
            if (dim.isEmpty) trackerMap -= loc.dim
        }
    }
  }

  def isLocationTracked(loc: Loc4): Boolean = {
    trackerMap.getOrElse(loc.dim, return false).getOrElse(loc.chunkCoords, return false).contains(loc)
  }

  def getTrackedLocationsInDim(dim: Int) = {
    trackerMap.get(dim).view.flatMap(_.values).flatten
  }

  def getTrackedLocations = {
    trackerMap.values.view.flatMap(_.values).flatten
  }

  def getLocationsInRange(loc: Loc4, range: Float): Iterable[Loc4] = {
    val radius      = Math.ceil(range / CHUNK_SIZE).toInt
    val chunkCoords = ChunkCoord(loc.getPos)
    var rsqr: Long  = 0
    try {
      rsqr = LongMath.checkedMultiply(radius, radius)
    }
    catch {
      case e: ArithmeticException => rsqr = -1
    }
    /*
    val chunks = if ((rsqr >= 0) && rsqr < trackerMap.get(loc.dim).map(_.size).getOrElse(0))
      getChunkCoordsInRadius(chunkCoords, radius)
    else
      getChunkCoordsInRadiusInDim(chunkCoords, radius, loc.dim)
    val locs   = chunks.view.flatMap(getLocationsInChunk(loc.dim, _)).filter(_.distSqr(loc) <= (range * range))
    locs
     */
    for {
      chunk <- if ((rsqr >= 0) && rsqr < trackerMap.get(loc.dim).map(_.size).getOrElse(0))
        getChunkCoordsInRadius(chunkCoords, radius)
      else
        getChunkCoordsInRadiusInDim(chunkCoords, radius, loc.dim)

      checkLoc <- getLocationsInChunk(loc.dim, chunk)
      if checkLoc.distSqr(loc) <= (range * range)
    } yield checkLoc
  }


  def getLocationsInRange(dim: Int, loc: (Float, Float, Float), range: Float): Iterable[Loc4] = {
    val (x, y, z)   = loc
    val chunkCoords = ChunkCoord(x.toInt >> 4, z.toInt >> 4)
    val radius      = Math.ceil(range / CHUNK_SIZE).toInt
    var rsqr: Long  = 0
    try {
      rsqr = LongMath.checkedMultiply(radius, radius)
    }
    catch {
      case e: ArithmeticException => rsqr = -1
    }
    for {
      chunk <- if ((rsqr >= 0) && rsqr < trackerMap.get(dim).map(_.size).getOrElse(0))
        getChunkCoordsInRadius(chunkCoords, radius)
      else
        getChunkCoordsInRadiusInDim(chunkCoords, radius, dim)
      checkLoc <- getLocationsInChunk(dim, chunk)
      if (((checkLoc.x - x) * (checkLoc.x - x)) + ((checkLoc.y - y) * (checkLoc.y - y)) + ((checkLoc.z - z) * (checkLoc.z - z))) <= (range * range)
    } yield checkLoc
  }

  def getLocationsInChunk(dim: Int, chunkLoc: ChunkCoord): Iterable[Loc4] = {
    trackerMap.getOrElse(dim, return Set[Loc4]()).getOrElse(chunkLoc, return Set[Loc4]())
  }

  private def getChunkCoordsInRadius(loc: ChunkCoord, radius: Int) = for {
    i <- -radius to radius
    j <- -radius to radius
  } yield ChunkCoord(loc.x + i, loc.z + j)

  private def getChunkCoordsInRadiusInDim(loc: ChunkCoord, radius: Int, dim: Int) = {
    trackerMap.getOrElse(dim, mutable.HashMap.empty).keys.filter { floc =>
      (floc.x >= (loc.x - radius) && floc.x <= (loc.x + radius)) &&
      (floc.z >= (loc.z - radius) && floc.z <= (loc.z + radius))
    }
  }

  def clear(): Unit = {
    trackerMap.clear()
  }

  def clearDim(id: Int): Unit = {
    trackerMap.remove(id)
  }
}
