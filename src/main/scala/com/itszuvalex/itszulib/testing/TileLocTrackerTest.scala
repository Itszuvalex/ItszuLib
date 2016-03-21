package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.core.TileEntityBase
import com.itszuvalex.itszulib.logistics.LocationTracker
import com.itszuvalex.itszulib.util.PlayerUtils
import net.minecraft.entity.player.EntityPlayer

/**
  * Created by Christopher Harris (Itszuvalex) on 8/3/15.
  */
object TileLocTrackerTest {
  val tracker = new LocationTracker
  val range   = 30f
}


class TileLocTrackerTest extends TileEntityBase {
  override def getMod: AnyRef = ItszuLib

  override def invalidate(): Unit = {
    super.invalidate()
    TileLocTrackerTest.tracker.removeLocation(getLoc)
  }

  override def validate(): Unit = {
    super.validate()
    TileLocTrackerTest.tracker.trackLocation(getLoc)
  }

  override def onSideActivate(par5EntityPlayer: EntityPlayer, side: Int): Boolean = {
    super.onSideActivate(par5EntityPlayer, side)
    val start = System.nanoTime()
    val locs = TileLocTrackerTest.tracker.getLocationsInRange(getLoc, TileLocTrackerTest.range)
    val end = System.nanoTime()
    val elapsed = end - start
    PlayerUtils.sendMessageToPlayer(par5EntityPlayer, "ItszuLib", locs.size + " tiles in range.")
    PlayerUtils.sendMessageToPlayer(par5EntityPlayer, "ItszuLib", "Query took: " + elapsed + " ns")
  }

  override def hasDescription: Boolean = false
}
