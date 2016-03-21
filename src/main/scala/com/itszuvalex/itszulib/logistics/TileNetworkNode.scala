package com.itszuvalex.itszulib.logistics

import com.itszuvalex.itszulib.api.core.Loc4

/**
  * Created by Christopher Harris (Itszuvalex) on 4/8/15.
  */
trait TileNetworkNode[C <: TileNetworkNode[C, T], T <: TileNetwork[C, T]] extends INetworkNode[T] {
  var network: T = null.asInstanceOf[T]

  override def canConnect(loc: Loc4): Boolean = getLoc.isNeighbor(loc)

  override def canAdd(iNetwork: INetwork[_, T]): Boolean = true

  override def added(iNetwork: INetwork[_, T]): Unit = {}

  override def removed(iNetwork: INetwork[_, T]): Unit = {}

  override def connect(node: Loc4): Unit = {}

  override def disconnect(node: Loc4): Unit = {}

  override def getNetwork: T = network

  //  override def getLoc = new Loc4(this)

  override def setNetwork(network: T): Unit = this.network = network

  override def refresh(): Unit = {}

}
