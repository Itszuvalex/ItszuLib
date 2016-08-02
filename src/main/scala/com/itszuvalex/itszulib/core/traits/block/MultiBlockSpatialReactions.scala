package com.itszuvalex.itszulib.core.traits.block

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.multiblock.IMultiBlockComponent

/**
  * Created by Itszuvalex on 1/1/15.
  */
trait MultiBlockSpatialReactions extends MultiBlock with SpatialReactions {
  override def onPickup(loc: Loc4): Unit = {
    loc.getWorld.get.getTileEntity(loc.getPos) match {
      case m: IMultiBlockComponent if m.getInfo.isValidMultiBlock => getMultiBlock.breakMultiBlock(m.getInfo.cLoc)
      case _ =>
    }
  }

  override def onPlacement(loc: Loc4): Unit = {
    getMultiBlock.formMultiBlockWithBlock(loc)
  }
}
