package com.itszuvalex.itszulib.logistics

import com.itszuvalex.itszulib.api.core.Loc4


/**
  * Created by Christopher Harris (Itszuvalex) on 4/5/15.
  */
trait INetworkNode[T <: INetwork[_, T]] {

  def setNetwork(network: T)

  def getNetwork: T

  def getLoc: Loc4

  def canConnect(loc: Loc4): Boolean

  def refresh(): Unit

  def canAdd(iNetwork: INetwork[_, T]): Boolean

  def added(iNetwork: INetwork[_, T]): Unit

  def removed(iNetwork: INetwork[_, T]): Unit

  def connect(node: Loc4): Unit

  def disconnect(node: Loc4): Unit

}
