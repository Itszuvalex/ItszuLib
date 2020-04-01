package com.itszuvalex.itszulib.logistics

import com.itszuvalex.itszulib.api.core.Loc4

/**
  * Created by Christopher Harris (Itszuvalex) on 4/8/15.
  */
trait TileNetworkNode[C <: TileNetworkNode[C, T], T <: TileNetwork[C, T]] extends INetworkNode[C, T] {
  var network: T = null.asInstanceOf[T]

  def getLoc: Loc4


  override def canConnect(node: C): Boolean = node match {
    case null => false
    case n => n.getLoc.isNeighbor(getLoc)
  }

  override def connect(node: C): Unit = {}

  override def disconnect(node: C): Unit = {}

  override def canAdd(iNetwork: T): Boolean = true

  override def added(iNetwork: T): Unit = {}

  override def removed(iNetwork: T): Unit = {}

  override def getNetwork: T = network

  //  override def getLoc = new Loc4(this)

  override def setNetwork(network: T): Unit = this.network = network

  override def refresh(): Unit = {}

}
