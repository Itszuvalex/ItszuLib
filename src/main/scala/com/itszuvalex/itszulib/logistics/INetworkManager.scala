package com.itszuvalex.itszulib.logistics

trait INetworkManager {

  def getNetwork(id: Int): Option[INetwork[_, _]]

  def removeNetwork(network: INetwork[_, _]): Unit

  def addNetwork(network: INetwork[_, _]): Unit

  def getNextID: Int

  def clear(): Unit

  def onTickEnd(): Unit

  def onTickStart(): Unit

  def networkCount: Int

  def networks: Iterable[INetwork[_, _]]
}
