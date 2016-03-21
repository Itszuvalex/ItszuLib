package com.itszuvalex.itszulib.networking

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.logistics.{INetwork, TileNetwork, TileNetworkNode}

/**
  * Created by Christopher Harris (Itszuvalex) on 4/14/15.
  */
class TestNetworking extends TestBase {

  trait Network {
    TestNetworkManager.networkMap.clear()
    TestNetworkManager.nextId = 0
    val network = new TestNetwork(TestNetworkManager.getID)
    network.register()
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
        network.create() shouldBe a[TestNetwork]
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

      /*
      Cannot test this.

      Reason being, relies upon worldObj().getTileEntity() calls, which cannot be easily tested in this environment.


    */


      /*

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
              TestNetworkManager.networkMap.contains(network.id) shouldBe false
            }

            "and" should {

              "should create 2 new networks" in new NetworkWithOrigin {
                val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
                val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
                network.addNode(neighbor)
                network.addNode(neighbor2)
                network.removeNode(neighbor)
               TestNetworkManager.networkMap.values.size shouldBe 2
              }

             "each should have 1 node" in new NetworkWithOrigin {
                val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
                val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
                network.addNode(neighbor)
                network.addNode(neighbor2)
                network.removeNode(neighbor)
               val networks = TestNetworkManager.networkMap.values.toArray
               networks(0).getNodes.size shouldBe 1
               networks(1).getNodes.size shouldBe 1
              }
              "each should have 0 edges" in new NetworkWithOrigin {
                val neighbor  = new TestNode(Loc4(1, 0, 0, 0))
                val neighbor2 = new TestNode(Loc4(2, 0, 0, 0))
                network.addNode(neighbor)
                network.addNode(neighbor2)
                network.removeNode(neighbor)
                val networks = TestNetworkManager.networkMap.values.toArray
                networks(0).getEdges.size shouldBe 0
                networks(1).getEdges.size shouldBe 0
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
      */

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

  class TestNetwork(_id: Int) extends TileNetwork[TestNode, TestNetwork](_id) {

    override def addConnection(a: Loc4, b: Loc4): Unit = {
      addConnectionSilently(a, b)
    }

    override def removeConnection(a: Loc4, b: Loc4): Unit = {
      removeConnectionSilently(a, b)
    }

    /**
      *
      * @return Create an empty new network of this type.
      */
    override def create() = new TestNetwork(TestNetworkManager.getID)

    /**
      * Called on networks by another network, when that network is incorporating this network.
      *
      * @param iNetwork Network that is taking over this network.
      */
    override def onTakeover(iNetwork: INetwork[TestNode, TestNetwork]): Unit = {}

    /**
      * Called on sub networks by a main network, when that network is splitting apart.
      *
      * @param iNetwork Network that will split into this sub network.
      */
    override def onSplit(iNetwork: INetwork[TestNode, TestNetwork]): Unit = {}

    /**
      * Called when a tick starts.
      */
    override def onTickStart(): Unit = {}

    override def unregister(): Unit = { TestNetworkManager.networkMap.remove(ID) }

    override def register(): Unit = { TestNetworkManager.networkMap(ID) = this }

    /**
      * Called when a tick ends.
      */
    override def onTickEnd(): Unit = {}
  }

  class TestNode(val loc: Loc4) extends TileNetworkNode[TestNode, TestNetwork] {
    override def getLoc: Loc4 = loc
  }

  object TestNetworkManager {
    val networkMap = scala.collection.mutable.HashMap[Int, TestNetwork]()
    var nextId     = 0

    def getID = { nextId += 1; nextId }
  }

}
