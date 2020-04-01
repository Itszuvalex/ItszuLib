package com.itszuvalex.itszulib.logistics


/**
  * Created by Christopher Harris (Itszuvalex) on 4/5/15.
  */
trait INetworkNode[C <: INetworkNode[C, T], T <: INetwork[C, T]] {

  def setNetwork(network: T)

  def getNetwork: T

  def canConnect(node: C): Boolean

  def refresh(): Unit

  def canAdd(iNetwork: T): Boolean

  def added(iNetwork: T): Unit

  def removed(iNetwork: T): Unit

  def connect(node: C): Unit

  def disconnect(node: C): Unit

}
