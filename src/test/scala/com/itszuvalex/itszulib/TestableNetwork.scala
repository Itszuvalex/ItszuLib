package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.logistics.{INetworkManager, ManagerNetwork, TileNetwork}

class TestableNetwork(_id: Int, val mod: IModule[TestableNetworkNode], val manager: INetworkManager) extends TileNetwork[TestableNetworkNode, TestableNetwork](_id) {

  override def register(): Unit = manager.addNetwork(this)

  override def unregister(): Unit = manager.removeNetwork(this)

  override def networkModule: IModule[TestableNetworkNode] = mod

  /*
  override def addConnection(a: Loc4, b: Loc4): Unit = {
    addConnectionSilently(a, b)
  }

  override def removeConnection(a: Loc4, b: Loc4): Unit = {
    removeConnectionSilently(a, b)
  }
  */

  /**
    * Called on networks by another network, when that network is incorporating this network.
    *
    * @param iNetwork Network that is taking over this network.
    */
  override def onTakeover(iNetwork: TestableNetwork): Unit = {}

  /**
    * Called on sub networks by a main network, when that network is splitting apart.
    *
    * @param iNetwork Network that will split into this sub network.
    */
  override def onSplit(iNetwork: TestableNetwork): Unit = {}

  /**
    * Called when a tick starts.
    */
  override def onTickStart(): Unit = {}

  /**
    * Called when a tick ends.
    */
  override def onTickEnd(): Unit = {}

  /**
    *
    * @return Create an empty new network of this type.
    */
  override def create(): TestableNetwork = new TestableNetwork(ManagerNetwork.instance.getNextID, mod, manager)
}
