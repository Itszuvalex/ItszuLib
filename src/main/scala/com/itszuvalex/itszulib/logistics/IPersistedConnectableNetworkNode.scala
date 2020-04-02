package com.itszuvalex.itszulib.logistics

import com.itszuvalex.itszulib.api.core.Loc4


/**
  * Created by Christopher Harris (Itszuvalex) on 4/5/15.
  */
trait IPersistedConnectableNetworkNode[C <: IPersistedConnectableNetworkNode[C, T], T <: INetwork[C, T]] extends INetworkNode[C, T] {

  def addPersistedConnection(node: Loc4): Unit

  def removePersistedConnection(node: Loc4): Unit
}
