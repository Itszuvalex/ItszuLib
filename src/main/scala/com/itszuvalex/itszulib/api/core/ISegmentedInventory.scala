package com.itszuvalex.itszulib.api.core

import java.util

import net.minecraft.inventory.ISidedInventory

/**
  * Created by Christopher on 2/5/2015.
  */
trait ISegmentedInventory extends ISidedInventory {

  def getSegments: util.Map[String, Array[Int]]
}
