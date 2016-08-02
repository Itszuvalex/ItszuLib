package com.itszuvalex.itszulib.core.traits.block

import com.itszuvalex.itszulib.api.core.Loc4

/**
  * Created by Itszuvalex on 1/1/15.
  */
trait SpatialReactions {

  def onPickup(loc: Loc4)

  def onPlacement(loc: Loc4)

}
