package com.itszuvalex.itszulib.core.traits.tile

import java.util

import com.itszuvalex.itszulib.api.core.ISegmentedInventory
import com.itszuvalex.itszulib.core.traits.tile.SegmentedInventory._

import scala.collection.JavaConversions._
import scala.collection._

/**
  * Created by Christopher on 2/5/2015.
  */

object SegmentedInventory {
  val FullInventory = "Inventory"
}

trait SegmentedInventory extends TileInventory with ISegmentedInventory {

  /**
    *
    * @return Map of segments mapped by segment name.  Array[Int] is the array of all indexes of inventory that are accessible by this segment.
    */
  override def getSegments: util.Map[String, Array[Int]] = Map(FullInventory -> inventory.getAccess.indices.toArray)
}
