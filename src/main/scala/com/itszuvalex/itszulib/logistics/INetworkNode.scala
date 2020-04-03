package com.itszuvalex.itszulib.logistics

import com.itszuvalex.itszulib.api.core.Loc4


/**
  * Created by Christopher Harris (Itszuvalex) on 4/5/15.
  */
trait INetworkNode[C <: INetworkNode[C, T], T <: INetwork[C, T]] {

  def setNetwork(network: T)

  def getNetwork: T

  def getLoc: Loc4

  def canConnect(loc: Loc4): Boolean

  def refresh(): Unit

  def canAdd(iNetwork: T): Boolean

  def onAdded(iNetwork: T): Unit

  def onRemoved(iNetwork: T): Unit

  def onConnect(node: Loc4): Unit

  def onDisconnect(node: Loc4): Unit

}
