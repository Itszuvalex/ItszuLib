package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.logistics.TileNetworkNode

class TestableNetworkNode(val loc: Loc4) extends TileNetworkNode[TestableNetworkNode, TestableNetwork] {
  override def getLoc: Loc4 = loc
}
