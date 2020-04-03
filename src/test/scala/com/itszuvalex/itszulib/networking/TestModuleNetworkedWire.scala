package com.itszuvalex.itszulib.networking

import com.itszuvalex.itszulib.api.core._
import com.itszuvalex.itszulib.api.wrappers.{IItemStack, ITileEntity}
import com.itszuvalex.itszulib.core.modules.ModuleNetworkedWire
import com.itszuvalex.itszulib.logistics.{INetworkManager, IPersistedConnectableNetworkNode, ManagerNetwork, TileNetwork}
import com.itszuvalex.itszulib.{TestBase, TestableNetworkNodeTileEntity, TestableWorld}
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class TestModuleNetworkedWire extends TestBase {

  trait Network {
    Module.clear()
    val TestableNetworkModule: IModule[TestableNetworkNodeTrait] = Module.registerModule("TestNetworkModule", null)
    val testableNetworkManager                                   = new ManagerNetwork
    testableNetworkManager.clear()
    val network = new TestableWiringNetwork(testableNetworkManager.getNextID, TestableNetworkModule, testableNetworkManager)
    network.register()
    val testableWorld = new TestableWorld(TestBase.getRandomWorldId)
    val originTE      = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(0, 0, 0)), testableWorld, null)
    val originNode    = new TestableNetworkWiringNode(originTE, TestableNetworkModule, testableNetworkManager)
    originTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(originNode))
    testableWorld.setITileEntity(originTE.getPos, originTE)
  }

  "ModuleNetworkedWire" should {
    "when placed have no connections" in new Network {
      EnumFacing.VALUES.foreach { f =>
        originNode.isBlocked(f) shouldBe false
        originNode.isConnected(f) shouldBe false
      }
    }
    "connect to nearby wires when placed" in new Network {
      val neighborTE   = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
      val neighborNode = new TestableNetworkWiringNode(neighborTE, TestableNetworkModule, testableNetworkManager)
      neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighborNode))
      testableWorld.setITileEntity(neighborTE.getPos, neighborTE)
      network.addNode(neighborNode)

      network.getNodes.size() shouldBe 1

      EnumFacing.VALUES.foreach { f =>
        originNode.isBlocked(f) shouldBe false
        originNode.isConnected(f) shouldBe false
        neighborNode.isBlocked(f) shouldBe false
        neighborNode.isConnected(f) shouldBe false
      }

      originNode.onBlockPlacedBy(testableWorld, new BlockPos(0, 0, 0), null, null, IItemStack.Empty)

      originNode.network.getNodes.size() shouldBe 2

      originNode.isConnected(EnumFacing.EAST) shouldBe true
      neighborNode.isConnected(EnumFacing.WEST) shouldBe true
    }
    "persist connection when one node chunk unloads" in new Network {
      val neighborTE   = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
      val neighborNode = new TestableNetworkWiringNode(neighborTE, TestableNetworkModule, testableNetworkManager)
      neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighborNode))
      testableWorld.setITileEntity(neighborTE.getPos, neighborTE)
      network.addNode(neighborNode)

      originNode.onBlockPlacedBy(testableWorld, new BlockPos(0, 0, 0), null, null, IItemStack.Empty)

      network.getNodes.size() shouldBe 2

      originNode.isConnected(EnumFacing.EAST) shouldBe true
      neighborNode.isConnected(EnumFacing.WEST) shouldBe true

      originNode.onChunkUnload(originTE)
      neighborNode.network.getNodes.size() shouldBe 1

      originNode.isConnected(EnumFacing.EAST) shouldBe true
      neighborNode.isConnected(EnumFacing.WEST) shouldBe true
    }
    "reconnect persisted connection when unloaded node reloads" in new Network {
      val neighborTE   = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
      val neighborNode = new TestableNetworkWiringNode(neighborTE, TestableNetworkModule, testableNetworkManager)
      neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighborNode))
      testableWorld.setITileEntity(neighborTE.getPos, neighborTE)
      network.addNode(neighborNode)

      originNode.onBlockPlacedBy(testableWorld, new BlockPos(0, 0, 0), null, null, IItemStack.Empty)

      network.getNodes.size() shouldBe 2

      originNode.isConnected(EnumFacing.EAST) shouldBe true
      neighborNode.isConnected(EnumFacing.WEST) shouldBe true

      originNode.onChunkUnload(originTE)
      network.getNodes.size() shouldBe 1

      originNode.isConnected(EnumFacing.EAST) shouldBe true
      neighborNode.isConnected(EnumFacing.WEST) shouldBe true

      originNode.network = null

      originNode.onLoad(originTE)
      neighborNode.network.getNodes.size() shouldBe 2
      originNode.isConnected(EnumFacing.EAST) shouldBe true
      neighborNode.isConnected(EnumFacing.WEST) shouldBe true
    }
    "disconnect from nearby wires when broken" in new Network {
      val neighborTE   = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
      val neighborNode = new TestableNetworkWiringNode(neighborTE, TestableNetworkModule, testableNetworkManager)
      neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighborNode))
      testableWorld.setITileEntity(neighborTE.getPos, neighborTE)
      network.addNode(neighborNode)

      network.getNodes.size() shouldBe 1

      EnumFacing.VALUES.foreach { f =>
        originNode.isBlocked(f) shouldBe false
        originNode.isConnected(f) shouldBe false
        neighborNode.isBlocked(f) shouldBe false
        neighborNode.isConnected(f) shouldBe false
      }

      originNode.onBlockPlacedBy(testableWorld, new BlockPos(0, 0, 0), null, null, IItemStack.Empty)

      network.getNodes.size() shouldBe 2

      originNode.isConnected(EnumFacing.EAST) shouldBe true
      neighborNode.isConnected(EnumFacing.WEST) shouldBe true

      originNode.onBlockBreak(originTE, null)
      testableWorld.setITileEntity(originTE.getPos, null)

      neighborNode.isConnected(EnumFacing.WEST) shouldBe false
      neighborNode.network.size shouldBe 1
      neighborNode.network.getEdges.size() shouldBe 0
    }
    "connect to and disconnect from machines that *should* connect that are themselves not wires" should {
      "connect to a non-wire machine that 'shouldConnect' when notified of neighbor change" in new Network {
        val machineTE    = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
        val neighborTE   = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        val neighborNode = new TestableNetworkWiringNode(neighborTE, TestableNetworkModule, testableNetworkManager) {
          override protected def shouldConnect(a: ITileEntity, f: EnumFacing): Boolean = super.shouldConnect(a, f) || a == machineTE

          override protected def getNetworkNodeFromITE(ite: ITileEntity, f: EnumFacing): Option[TestableNetworkNodeTrait] = {
            if (ite.hasModule(mod, null)) Some(ite.getModule(mod, null)) else None
          }
        }
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighborNode))
        testableWorld.setITileEntity(neighborTE.getPos, neighborTE)
        network.addNode(neighborNode)

        network.getNodes.size() shouldBe 1

        EnumFacing.VALUES.foreach { f =>
          originNode.isBlocked(f) shouldBe false
          originNode.isConnected(f) shouldBe false
          neighborNode.isBlocked(f) shouldBe false
          neighborNode.isConnected(f) shouldBe false
        }

        originNode.onBlockPlacedBy(testableWorld, new BlockPos(0, 0, 0), null, null, IItemStack.Empty)

        network.getNodes.size() shouldBe 2

        originNode.isConnected(EnumFacing.EAST) shouldBe true
        neighborNode.isConnected(EnumFacing.WEST) shouldBe true

        testableWorld.setITileEntity(machineTE.getPos, machineTE)
        neighborNode.onNeighborChanged(testableWorld, neighborTE.getPos, null, null, machineTE.getPos)

        neighborNode.isConnected(EnumFacing.EAST) shouldBe true
        network.getNodes.size() shouldBe 2

        testableWorld.setITileEntity(machineTE.getPos, null)
        neighborNode.onNeighborChanged(testableWorld, neighborTE.getPos, null, null, machineTE.getPos)

        neighborNode.isConnected(EnumFacing.EAST) shouldBe false
        network.getNodes.size() shouldBe 2
      }
    }
  }

  trait TestableNetworkNodeTrait extends IPersistedConnectableNetworkNode[TestableNetworkNodeTrait, TestableWiringNetwork]

  class TestableNetworkWiringNode(tile: ITileEntity, val mod: IModule[TestableNetworkNodeTrait], manager: INetworkManager)
    extends ModuleNetworkedWire[TestableNetworkNodeTrait, TestableWiringNetwork](tile, () => new TestableWiringNetwork(manager.getNextID, mod, manager))
      with TestableNetworkNodeTrait {
    override protected def shouldConnect(a: ITileEntity, f: EnumFacing): Boolean = a.hasModule(mod, null)

    override protected def getNetworkNodeFromITE(ite: ITileEntity, f: EnumFacing): Option[TestableNetworkNodeTrait] = Some(ite.getModule(mod, null))

    override def module: IModule[TestableNetworkNodeTrait] = mod

    override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[TestableNetworkWiringNode] = _ => Some(this)
  }

  class TestableWiringNetwork(_id: Int, val mod: IModule[TestableNetworkNodeTrait], val manager: INetworkManager) extends TileNetwork[TestableNetworkNodeTrait, TestableWiringNetwork](_id) {

    override def register(): Unit = manager.addNetwork(this)

    override def unregister(): Unit = manager.removeNetwork(this)

    override def networkModule: IModule[TestableNetworkNodeTrait] = mod

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
    override def create(): TestableWiringNetwork = new TestableWiringNetwork(ManagerNetwork.instance.getNextID, mod, manager)
  }

}
