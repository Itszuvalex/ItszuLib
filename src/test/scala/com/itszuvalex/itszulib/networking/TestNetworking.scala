package com.itszuvalex.itszulib.networking

import com.itszuvalex.itszulib.api.core._
import com.itszuvalex.itszulib.api.wrappers.IWorld
import com.itszuvalex.itszulib.logistics.INetwork.Edge
import com.itszuvalex.itszulib.logistics.ManagerNetwork
import com.itszuvalex.itszulib._
import net.minecraft.util.math.BlockPos

import scala.collection.JavaConversions._

/**
  * Created by Christopher Harris (Itszuvalex) on 4/14/15.
  */
class TestNetworking extends TestBase {

  trait Network {
    Module.clear()
    val TestableNetworkModule: IModule[TestableNetworkNode] = Module.registerModule("TestNetworkModule", null)
    val testableNetworkManager                              = new ManagerNetwork
    testableNetworkManager.clear()
    val network = new TestableNetwork(testableNetworkManager.getNextID, TestableNetworkModule, testableNetworkManager)
    network.register()
    val testableWorld = new TestableWorld(TestBase.getRandomWorldId)
  }

  trait NetworkWithOrigin extends Network {
    val origin   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(0, 0, 0)))
    val originTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(0, 0, 0)), testableWorld, null)
    originTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(origin))
    testableWorld.setITileEntity(new BlockPos(0, 0, 0), originTE)
    network.addNode(origin)
  }

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
        val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
        val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
        network.addNode(neighbor)
        val nodes = network.getNodes
        nodes.size() shouldBe 2
        nodes should contain allOf(origin, neighbor)
      }

      "have 1 edge" in new NetworkWithOrigin {
        val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
        val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
        network.addNode(neighbor)
        val edges = network.getEdges
        edges.size() shouldBe 1
        edges should contain(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))))
      }
    }

    "creating" should {
      "make a network of the same type" in new Network {
        network.create() shouldBe a[TestableNetwork]
      }
    }

    "adding two connectable nodes linearly" should {
      "have 3 nodes" in new NetworkWithOrigin {
        val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
        val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
        val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
        val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
        neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val nodes = network.getNodes
        nodes.size() shouldBe 3
        nodes should contain allOf(origin, neighbor, neighbor2)
      }

      "have 2 edges" in new NetworkWithOrigin {
        val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
        val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
        val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
        val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
        neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val edges = network.getEdges
        edges.size() shouldBe 2
        edges should contain allOf(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))), Edge(new Loc4(testableWorld, new BlockPos(1, 0, 0)), new Loc4(testableWorld, new BlockPos(2, 0, 0))))
      }

      "when removing nodes" should {
        "on edge" should {
          "have 2 nodes" in new NetworkWithOrigin {
            val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
            val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
            neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
            testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
            val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
            val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
            neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
            testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
            network.addNode(neighbor)
            network.addNode(neighbor2)
            network.removeNode(neighbor2)
            val nodes = network.getNodes
            nodes.size() shouldBe 2
            nodes should contain allOf(origin, neighbor)
          }

          "have 1 edge" in new NetworkWithOrigin {
            val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
            val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
            neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
            testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
            val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
            val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
            neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
            testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
            network.addNode(neighbor)
            network.addNode(neighbor2)
            network.removeNode(neighbor2)
            val edges = network.getEdges
            edges.size() shouldBe 1
            edges should contain(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))))
          }
        }


        "on center" should {
          "split and after splitting" should {
            "be empty" in new NetworkWithOrigin {
              val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
              val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
              neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
              testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
              val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
              val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
              neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
              testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
              network.addNode(neighbor)
              network.addNode(neighbor2)
              network.removeNode(neighbor)
              network.getNodes.isEmpty shouldBe true
              network.getEdges.isEmpty shouldBe true

            }

            "not be registered" in new NetworkWithOrigin {
              val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
              val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
              neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
              testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
              val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
              val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
              neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
              testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
              network.addNode(neighbor)
              network.addNode(neighbor2)
              network.removeNode(neighbor)
              network.getNodes.isEmpty shouldBe true
              network.getEdges.isEmpty shouldBe true
              testableNetworkManager.getNetwork(network.id) shouldBe None
            }

            "and" should {

              "should create 2 new networks" in new NetworkWithOrigin {
                val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
                val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
                neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
                testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
                val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
                val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
                neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
                testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
                network.addNode(neighbor)
                network.addNode(neighbor2)
                network.removeNode(neighbor)
                neighbor2.getNetwork shouldNot be theSameInstanceAs origin.getNetwork
              }

              "each should have 1 node" in new NetworkWithOrigin {
                val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
                val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
                neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
                testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
                val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
                val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
                neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
                testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
                network.addNode(neighbor)
                network.addNode(neighbor2)
                network.removeNode(neighbor)

                neighbor2.getNetwork != origin.getNetwork
                origin.getNetwork.size shouldBe 1
                neighbor2.getNetwork.size shouldBe 1
              }
              "each should have 0 edges" in new NetworkWithOrigin {
                val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
                val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
                neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
                testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
                val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
                val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
                neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
                testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
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
            val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
            val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
            neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
            testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
            val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
            val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
            neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
            testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
            network.addNode(neighbor)
            network.addNode(neighbor2)
            network.removeNodes(List(neighbor, neighbor2))
            val nodes = network.getNodes
            nodes.size() shouldBe 1
            nodes should contain(origin)
          }

          "have 0 edges" in new NetworkWithOrigin {
            val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
            val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
            neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
            testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
            val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
            val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
            neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
            testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
            network.addNode(neighbor)
            network.addNode(neighbor2)
            network.removeNodes(List(neighbor, neighbor2))
            val edges = network.getEdges
            edges.size() shouldBe 0
          }
        }
      }

      "takeover by nodes" in new NetworkWithOrigin {
        val network2 = new TestableNetwork(testableNetworkManager.getNextID, TestableNetworkModule, testableNetworkManager)
        network2.register()
        val network3 = new TestableNetwork(testableNetworkManager.getNextID, TestableNetworkModule, testableNetworkManager)
        network3.register()
        val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
        val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
        val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
        val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
        neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
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
        edges should contain(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))))

        network.addConnectionNodes(neighbor, neighbor2)

        network3.getNodes.size() shouldBe 0
        network3.getEdges.size() shouldBe 0
        testableNetworkManager.getNetwork(network3.id) shouldBe None

        neighbor2.network should be theSameInstanceAs network

        nodes = network.getNodes
        edges = network.getEdges

        nodes.size() shouldBe 3
        nodes should contain allOf(origin, neighbor, neighbor2)
        edges should contain allOf(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))), Edge(new Loc4(testableWorld, new BlockPos(1, 0, 0)), new Loc4(testableWorld, new BlockPos(2, 0, 0))))
      }

      "takeover by loc" in new NetworkWithOrigin {
        val network2 = new TestableNetwork(testableNetworkManager.getNextID, TestableNetworkModule, testableNetworkManager)
        network2.register()
        val network3 = new TestableNetwork(testableNetworkManager.getNextID, TestableNetworkModule, testableNetworkManager)
        network3.register()
        val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
        val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
        val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(2, 0, 0)))
        val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)), testableWorld, null)
        neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(2, 0, 0)).getPos, neighbor2TE)
        network2.addNode(neighbor)
        network3.addNode(neighbor2)

        network.addConnection(origin.getLoc, neighbor.getLoc)

        network2.getNodes.size() shouldBe 0
        network2.getEdges.size() shouldBe 0
        testableNetworkManager.getNetwork(network2.id) shouldBe None

        neighbor.network should be theSameInstanceAs network

        var nodes = network.getNodes
        var edges = network.getEdges
        nodes.size() shouldBe 2
        nodes should contain allOf(origin, neighbor)
        edges should contain(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))))

        network.addConnection(neighbor.getLoc, neighbor2.getLoc)

        network3.getNodes.size() shouldBe 0
        network3.getEdges.size() shouldBe 0
        testableNetworkManager.getNetwork(network3.id) shouldBe None

        neighbor2.network should be theSameInstanceAs network

        nodes = network.getNodes
        edges = network.getEdges

        nodes.size() shouldBe 3
        nodes should contain allOf(origin, neighbor, neighbor2)
        edges should contain allOf(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))), Edge(new Loc4(testableWorld, new BlockPos(1, 0, 0)), new Loc4(testableWorld, new BlockPos(2, 0, 0))))
      }
    }
    "adding two connectable nodes in L" should {
      "have 3 nodes" in new NetworkWithOrigin {
        val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
        val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
        val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(0, 1, 0)))
        val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(0, 1, 0)), testableWorld, null)
        neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(0, 1, 0)).getPos, neighbor2TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val nodes = network.getNodes
        nodes.size() shouldBe 3
        nodes should contain allOf(origin, neighbor, neighbor2)
      }

      "have 2 edges" in new NetworkWithOrigin {
        val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
        val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
        val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(0, 1, 0)))
        val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(0, 1, 0)), testableWorld, null)
        neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(0, 1, 0)).getPos, neighbor2TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val edges = network.getEdges
        edges.size() shouldBe 2
        edges should contain allOf(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))), Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(0, 1, 0))))
      }
    }
    "adding three connectable nodes in square" should {
      "have 4 nodes" in new NetworkWithOrigin {
        val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
        val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
        val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(0, 1, 0)))
        val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(0, 1, 0)), testableWorld, null)
        neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(0, 1, 0)).getPos, neighbor2TE)
        val neighbor3   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 1, 0)))
        val neighbor3TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 1, 0)), testableWorld, null)
        neighbor3TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor3))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 1, 0)).getPos, neighbor3TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        network.addNode(neighbor3)
        val nodes = network.getNodes
        nodes.size() shouldBe 4
        nodes should contain allOf(origin, neighbor, neighbor2, neighbor3)
      }

      "have 4 edges" in new NetworkWithOrigin {
        val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
        val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
        val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(0, 1, 0)))
        val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(0, 1, 0)), testableWorld, null)
        neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(0, 1, 0)).getPos, neighbor2TE)
        val neighbor3   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 1, 0)))
        val neighbor3TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 1, 0)), testableWorld, null)
        neighbor3TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor3))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 1, 0)).getPos, neighbor3TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        network.addNode(neighbor3)
        val edges = network.getEdges
        edges.size() shouldBe 4
        edges should contain allOf(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))), Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(0, 1, 0))), Edge(new Loc4(testableWorld, new BlockPos(1, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 1, 0))),
          Edge(new Loc4(testableWorld, new BlockPos(0, 1, 0)), new Loc4(testableWorld, new BlockPos(1, 1, 0))))
      }

      "when 2 connections removed should split" in new NetworkWithOrigin {
        val neighbor   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 0, 0)))
        val neighborTE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)), testableWorld, null)
        neighborTE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 0, 0)).getPos, neighborTE)
        val neighbor2   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(0, 1, 0)))
        val neighbor2TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(0, 1, 0)), testableWorld, null)
        neighbor2TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor2))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(0, 1, 0)).getPos, neighbor2TE)
        val neighbor3   = new TestableNetworkNode(new Loc4(testableWorld, new BlockPos(1, 1, 0)))
        val neighbor3TE = new TestableNetworkNodeTileEntity(new Loc4(testableWorld, new BlockPos(1, 1, 0)), testableWorld, null)
        neighbor3TE.moduleCapabilityMap.addModule(TestableNetworkModule, _ => Some(neighbor3))
        testableWorld.setITileEntity(new Loc4(testableWorld, new BlockPos(1, 1, 0)).getPos, neighbor3TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        network.addNode(neighbor3)
        var nodes = network.getNodes
        nodes.size() shouldBe 4
        nodes should contain allOf(origin, neighbor, neighbor2, neighbor3)
        var edges = network.getEdges
        edges.size() shouldBe 4
        edges should contain allOf(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))), Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(0, 1, 0))), Edge(new Loc4(testableWorld, new BlockPos(1, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 1, 0))),
          Edge(new Loc4(testableWorld, new BlockPos(0, 1, 0)), new Loc4(testableWorld, new BlockPos(1, 1, 0))))

        network.removeConnection(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(0, 1, 0)))
        nodes = network.getNodes
        nodes.size() shouldBe 4
        nodes should contain allOf(origin, neighbor, neighbor2, neighbor3)
        edges = network.getEdges
        edges.size() shouldBe 3
        edges should contain allOf(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))), Edge(new Loc4(testableWorld, new BlockPos(1, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 1, 0))), Edge(new Loc4(testableWorld, new BlockPos(0, 1, 0)), new Loc4(testableWorld, new BlockPos(1, 1, 0))))

        network.removeConnection(new Loc4(testableWorld, new BlockPos(1, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 1, 0)))
        origin.network should be theSameInstanceAs neighbor.network
        val newNetwork = origin.getNetwork
        nodes = newNetwork.getNodes
        nodes.size() shouldBe 2
        nodes should contain allOf(origin, neighbor)
        edges = newNetwork.getEdges
        edges.size() shouldBe 1
        edges should contain(Edge(new Loc4(testableWorld, new BlockPos(0, 0, 0)), new Loc4(testableWorld, new BlockPos(1, 0, 0))))

        neighbor2.network should be theSameInstanceAs neighbor3.network
        val network2 = neighbor2.network
        nodes = network2.getNodes
        nodes.size() shouldBe 2
        nodes should contain allOf(neighbor2, neighbor3)
        edges = network2.getEdges
        edges.size() shouldBe 1
        edges should contain(Edge(new Loc4(testableWorld, new BlockPos(0, 1, 0)), new Loc4(testableWorld, new BlockPos(1, 1, 0))))

        newNetwork shouldNot be theSameInstanceAs network2

        testableNetworkManager.networks.size() shouldBe 2
        testableNetworkManager.getNetwork(network.id) shouldBe None
      }
    }
  }
}
