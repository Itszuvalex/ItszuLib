package com.itszuvalex.itszulib.networking

import com.itszuvalex.itszulib.api.core._
import com.itszuvalex.itszulib.api.wrappers.{ITileEntity, IWorld}
import com.itszuvalex.itszulib.core.modules.ModuleNetworkedWire
import com.itszuvalex.itszulib.logistics.{INetworkManager, ManagerNetwork, TileNetwork}
import com.itszuvalex.itszulib.{TestBase, TestableWorld}
import net.minecraft.util.EnumFacing

class TestModuleNetworkedWire extends TestBase {

  "ModuleNetworkedWire" should {
    trait Network {
      Module.clear()
      val TestableNetworkModule: IModule[TestableNetworkWiringNode] = Module.registerModule("TestNetworkModule", null)
      val testableNetworkManager                                    = new ManagerNetwork
      testableNetworkManager.clear()
      ManagerNetwork.setInstance(testableNetworkManager)
      val network = new TestableWiringNetwork(testableNetworkManager.getNextID, TestableNetworkModule)
      network.register()
      val testableWorld = new TestableWorld(0)
      Loc4.OverrideDimensionMapper = Some(new DimensionMapper {
        override def dimensionIdForWorld(w: IWorld): Int = testableWorld.dimensionId

        override def worldForDimensionId(i: Int): IWorld = testableWorld
      })
    }
  }

  class TestableNetworkWiringNode(tile: ITileEntity, mod: IModule[TestableNetworkWiringNode], manager: INetworkManager) extends ModuleNetworkedWire[TestableNetworkWiringNode, TestableWiringNetwork](tile, () => new TestableWiringNetwork(manager.getNextID, mod)) {
    override protected def shouldConnect(a: ITileEntity): Boolean = true

    override protected def getNetworkNodeFromITE(ite: ITileEntity): TestableNetworkWiringNode = ite.getModule(mod, null)

    override def module: IModule[TestableNetworkWiringNode] = mod

    override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[TestableNetworkWiringNode] = _ => Some(this)
  }

  class TestableWiringNetwork(_id: Int, mod: IModule[TestableNetworkWiringNode]) extends TileNetwork[TestableNetworkWiringNode, TestableWiringNetwork](_id) {

    override def networkModule: IModule[TestableNetworkWiringNode] = mod

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
    override def onTakeover(iNetwork: TestableWiringNetwork): Unit = {}

    /**
      * Called on sub networks by a main network, when that network is splitting apart.
      *
      * @param iNetwork Network that will split into this sub network.
      */
    override def onSplit(iNetwork: TestableWiringNetwork): Unit = {}

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
    override def create(): TestableWiringNetwork = new TestableWiringNetwork(ManagerNetwork.instance.getNextID, mod)
  }

}
