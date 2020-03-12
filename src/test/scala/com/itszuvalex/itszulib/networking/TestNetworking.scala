package com.itszuvalex.itszulib.networking

import com.itszuvalex.itszulib.api.core.{DimensionMapper, Loc4}
import com.itszuvalex.itszulib.api.wrappers.IWorld
import com.itszuvalex.itszulib.logistics.{ManagerNetwork, TileNetwork, TileNetworkNode}
import com.itszuvalex.itszulib.{TestBase, TestableWorld}
import net.minecraftforge.common.capabilities.Capability

import scala.collection.JavaConversions._

/**
  * Created by Christopher Harris (Itszuvalex) on 4/14/15.
  */
class TestNetworking extends TestBase {

  trait Network {
    val testableNetworkManager = new ManagerNetwork
    testableNetworkManager.clear()
    ManagerNetwork.setInstance(testableNetworkManager)
    val network = new TestableNetwork(testableNetworkManager.getNextID)
    network.register()
    val testableWorld = new TestableWorld(0)
    Loc4.OverrideDimensionMapper = Some(new DimensionMapper {
      override def dimensionIdForWorld(w: IWorld): Int = testableWorld.dimensionId

      override def worldForDimensionId(i: Int): IWorld = testableWorld
    })
  }

  trait OriginNode {
    val origin = new TestNode(Loc4(0, 0, 0, 0))
  }

  trait NetworkWithOrigin extends Network with OriginNode {network.addNode(origin)}

  "A Network" when {
    "constructing" should {
      "construct" in new Network {
      }
      "have no nodes" in new Network {
        network.getNodes.isEmpty shouldBe true
      }
      "have no connections" in new Network {
        network.getConnections.isEmpty shouldBe true
        network.getEdges.isEmpty shouldBe true
      }
    }

    "adding a node" should {
      "have 1 node" in new NetworkWithOrigin {
        val nodes = network.getNodes
        nodes.size() shouldBe 1
      }

      "have the added node" in new NetworkWithOrigin {
        val nodes = network.getNodes
        nodes should contain(origin)
      }
    }

    "adding connectable node" should {
      "have 2 nodes" in new NetworkWithOrigin {
        val neighbor = new TestNode(Loc4(1, 0, 0, 0))
        network.addNode(neighbor)
        val nodes = network.getNodes
        nodes.size() shouldBe 2
        nodes should contain allOf(origin, neighbor)
      }

      "have 1 edge" in new NetworkWithOrigin {
        val neighbor = new TestNode(Loc4(1, 0, 0, 0))
        network.addNode(neighbor)
        val edges = network.getEdges
        edges.size() shouldBe 1
        edges should contain(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0))
      }
    }

    "creating" should {
      "make a network of the same type" in new Network {
        network.create() shouldBe a[TestableNetwork]
      }
    }

    "adding two connectable nodes linearly" should {
      "have 3 nodes" in new NetworkWithOrigin {
        val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
        val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val nodes = network.getNodes
        nodes.size() shouldBe 3
        nodes should contain allOf(origin, neighbor, neighbor2)
      }

      "have 2 edges" in new NetworkWithOrigin {
        val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
        val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val edges = network.getEdges
        edges.size() shouldBe 2
        edges should contain allOf(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0), Loc4(1, 0, 0, 0) -> Loc4(2, 0, 0, 0))
      }

      "when removing nodes" should {
        "on edge" should {
          "have 2 nodes" in new NetworkWithOrigin {
            val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
            val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
            network.addNode(neighbor)
            network.addNode(neighbor2)
            network.removeNode(neighbor2)
            val nodes = network.getNodes
            nodes.size() shouldBe 2
            nodes should contain allOf(origin, neighbor)
          }

          "have 1 edge" in new NetworkWithOrigin {
            val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
            val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
            network.addNode(neighbor)
            network.addNode(neighbor2)
            network.removeNode(neighbor2)
            val edges = network.getEdges
            edges.size() shouldBe 1
            edges should contain(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0))
          }
        }


        "on center" should {
          "split and after splitting" should {
            "be empty" in new NetworkWithOrigin {
              val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
              val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
              network.addNode(neighbor)
              network.addNode(neighbor2)
              network.removeNode(neighbor)
              network.getNodes.isEmpty shouldBe true
              network.getEdges.isEmpty shouldBe true

            }

            "not be registered" in new NetworkWithOrigin {
              val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
              val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
              network.addNode(neighbor)
              network.addNode(neighbor2)
              network.removeNode(neighbor)
              network.getNodes.isEmpty shouldBe true
              network.getEdges.isEmpty shouldBe true
              testableNetworkManager.getNetwork(network.id) shouldBe None
            }

            "and" should {

              "should create 2 new networks" in new NetworkWithOrigin {
                val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
                val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
                network.addNode(neighbor)
                network.addNode(neighbor2)
                network.removeNode(neighbor)
                testableNetworkManager.networkCount shouldBe 2
              }

              "each should have 1 node" in new NetworkWithOrigin {
                val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
                val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
                network.addNode(neighbor)
                network.addNode(neighbor2)
                network.removeNode(neighbor)
                val networks = testableNetworkManager.networks
                networks.foreach(_.getNodes.size shouldBe 1)
              }
              "each should have 0 edges" in new NetworkWithOrigin {
                val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
                val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
                network.addNode(neighbor)
                network.addNode(neighbor2)
                network.removeNode(neighbor)
                val networks = testableNetworkManager.networks
                networks.foreach(_.getEdges.size shouldBe 0)
              }
            }
          }
        }

        "two at a time" should {
          "have 1 nodes" in new NetworkWithOrigin {
            val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
            val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
            network.addNode(neighbor)
            network.addNode(neighbor2)
            network.removeNodes(List(neighbor, neighbor2))
            val nodes = network.getNodes
            nodes.size() shouldBe 1
            nodes should contain(origin)
          }

          "have 0 edges" in new NetworkWithOrigin {
            val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
            val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
            network.addNode(neighbor)
            network.addNode(neighbor2)
            network.removeNodes(List(neighbor, neighbor2))
            val edges = network.getEdges
            edges.size() shouldBe 0
          }
        }
      }

      "takeover" in new NetworkWithOrigin {
        val network2 = new TestableNetwork(testableNetworkManager.getNextID)
        network2.register()
        val network3 = new TestableNetwork(testableNetworkManager.getNextID)
        network3.register()
        val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
        val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
        network2.addNode(neighbor)
        network3.addNode(neighbor2)

        network.addConnectionNodes(origin, neighbor)

        network2.getNodes.size() shouldBe 0
        network2.getEdges.size() shouldBe 0
        testableNetworkManager.getNetwork(network2.id) shouldBe None

        neighbor.network should be theSameInstanceAs network

        var nodes = network.getNodes
        var edges = network.getEdges
        nodes.size() shouldBe 2
        nodes should contain allOf(origin, neighbor)
        edges should contain(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0))

        network.addConnectionNodes(neighbor, neighbor2)

        network3.getNodes.size() shouldBe 0
        network3.getEdges.size() shouldBe 0
        testableNetworkManager.getNetwork(network3.id) shouldBe None

        neighbor2.network should be theSameInstanceAs network

        nodes = network.getNodes
        edges = network.getEdges

        nodes.size() shouldBe 3
        nodes should contain allOf(origin, neighbor, neighbor2)
        edges should contain allOf(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0), Loc4(1, 0, 0, 0) -> Loc4(2, 0, 0, 0))
      }
    }
    "adding two connectable nodes in L" should {
      "have 3 nodes" in new NetworkWithOrigin {
        val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
        val neighbor2 = new TestNode(Loc4(0, 1, 0, 0))
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val nodes = network.getNodes
        nodes.size() shouldBe 3
        nodes should contain allOf(origin, neighbor, neighbor2)
      }

      "have 2 edges" in new NetworkWithOrigin {
        val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
        val neighbor2 = new TestNode(Loc4(0, 1, 0, 0))
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val edges = network.getEdges
        edges.size() shouldBe 2
        edges should contain allOf(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0), Loc4(0, 0, 0, 0) -> Loc4(0, 1, 0, 0))
      }
    }
    "adding three connectable nodes in square" should {
      "have 4 nodes" in new NetworkWithOrigin {
        val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
        val neighbor2 = new TestNode(Loc4(0, 1, 0, 0))
        val neighbor3 = new TestNode(Loc4(1, 1, 0, 0))
        network.addNode(neighbor)
        network.addNode(neighbor2)
        network.addNode(neighbor3)
        val nodes = network.getNodes
        nodes.size() shouldBe 4
        nodes should contain allOf(origin, neighbor, neighbor2, neighbor3)
      }

      "have 4 edges" in new NetworkWithOrigin {
        val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
        val neighbor2 = new TestNode(Loc4(0, 1, 0, 0))
        val neighbor3 = new TestNode(Loc4(1, 1, 0, 0))
        network.addNode(neighbor)
        network.addNode(neighbor2)
        network.addNode(neighbor3)
        val edges = network.getEdges
        edges.size() shouldBe 4
        edges should contain allOf(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0), Loc4(0, 0, 0, 0) -> Loc4(0, 1, 0, 0), Loc4(1, 0, 0, 0) -> Loc4(1, 1, 0, 0),
          Loc4(0, 1, 0, 0) -> Loc4(1, 1, 0, 0))
      }
    }
  }


  /*
  TEST CLASS EXTENSIONS
   */

  class TestableNetwork(_id: Int) extends TileNetwork[TestNode, TestableNetwork](_id) {

    override def networkCapability: Capability[TestNode] = null

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
    override def create(): TestableNetwork = new TestableNetwork(ManagerNetwork.instance.getNextID)
  }

  class TestNode(val loc: Loc4) extends TileNetworkNode[TestNode, TestableNetwork] {
    override def getLoc: Loc4 = loc
  }

}
