package com.itszuvalex.itszulib.networking

import com.itszuvalex.itszulib.api.core._
import com.itszuvalex.itszulib.api.wrappers.{ITileEntity, IWorld}
import com.itszuvalex.itszulib.logistics.{ManagerNetwork, TileNetwork, TileNetworkNode}
import com.itszuvalex.itszulib.{TestBase, TestableWorld}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.capabilities.Capability

import scala.collection.JavaConversions._

/**
  * Created by Christopher Harris (Itszuvalex) on 4/14/15.
  */
class TestNetworking extends TestBase {

  trait Network {
    Module.clear()
    val TestableNetworkModule: IModule[TestNode] = Module.registerModule("TestNetworkModule", null)
    val testableNetworkManager                   = new ManagerNetwork
    testableNetworkManager.clear()
    ManagerNetwork.setInstance(testableNetworkManager)
    val network = new TestableNetwork(testableNetworkManager.getNextID, TestableNetworkModule)
    network.register()
    val testableWorld = new TestableWorld(0)
    Loc4.OverrideDimensionMapper = Some(new DimensionMapper {
      override def dimensionIdForWorld(w: IWorld): Int = testableWorld.dimensionId

      override def worldForDimensionId(i: Int): IWorld = testableWorld
    })
  }

  trait NetworkWithOrigin extends Network {
    val origin   = new TestNode(Loc4(0, 0, 0, 0))
    val originTE = new TestableTileEntity(Loc4(0, 0, 0, 0), testableWorld, origin, TestableNetworkModule)
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
        val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
        val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
        network.addNode(neighbor)
        val nodes = network.getNodes
        nodes.size() shouldBe 2
        nodes should contain allOf(origin, neighbor)
      }

      "have 1 edge" in new NetworkWithOrigin {
        val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
        val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
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
        val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
        val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
        val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
        val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val nodes = network.getNodes
        nodes.size() shouldBe 3
        nodes should contain allOf(origin, neighbor, neighbor2)
      }

      "have 2 edges" in new NetworkWithOrigin {
        val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
        val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
        val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
        val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val edges = network.getEdges
        edges.size() shouldBe 2
        edges should contain allOf(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0), Loc4(1, 0, 0, 0) -> Loc4(2, 0, 0, 0))
      }

      "when removing nodes" should {
        "on edge" should {
          "have 2 nodes" in new NetworkWithOrigin {
            val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
            val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
            testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
            val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
            val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
            testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
            network.addNode(neighbor)
            network.addNode(neighbor2)
            network.removeNode(neighbor2)
            val nodes = network.getNodes
            nodes.size() shouldBe 2
            nodes should contain allOf(origin, neighbor)
          }

          "have 1 edge" in new NetworkWithOrigin {
            val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
            val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
            testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
            val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
            val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
            testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
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
              val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
              val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
              testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
              val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
              val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
              testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
              network.addNode(neighbor)
              network.addNode(neighbor2)
              network.removeNode(neighbor)
              network.getNodes.isEmpty shouldBe true
              network.getEdges.isEmpty shouldBe true

            }

            "not be registered" in new NetworkWithOrigin {
              val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
              val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
              testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
              val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
              val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
              testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
              network.addNode(neighbor)
              network.addNode(neighbor2)
              network.removeNode(neighbor)
              network.getNodes.isEmpty shouldBe true
              network.getEdges.isEmpty shouldBe true
              testableNetworkManager.getNetwork(network.id) shouldBe None
            }

            "and" should {

              "should create 2 new networks" in new NetworkWithOrigin {
                val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
                val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
                testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
                val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
                val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
                testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
                network.addNode(neighbor)
                network.addNode(neighbor2)
                network.removeNode(neighbor)
                testableNetworkManager.networkCount shouldBe 2
              }

              "each should have 1 node" in new NetworkWithOrigin {
                val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
                val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
                testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
                val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
                val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
                testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
                network.addNode(neighbor)
                network.addNode(neighbor2)
                network.removeNode(neighbor)
                val networks = testableNetworkManager.networks
                networks.foreach(_.getNodes.size shouldBe 1)
              }
              "each should have 0 edges" in new NetworkWithOrigin {
                val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
                val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
                testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
                val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
                val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
                testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
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
            val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
            val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
            testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
            val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
            val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
            testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
            network.addNode(neighbor)
            network.addNode(neighbor2)
            network.removeNodes(List(neighbor, neighbor2))
            val nodes = network.getNodes
            nodes.size() shouldBe 1
            nodes should contain(origin)
          }

          "have 0 edges" in new NetworkWithOrigin {
            val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
            val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
            testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
            val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
            val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
            testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
            network.addNode(neighbor)
            network.addNode(neighbor2)
            network.removeNodes(List(neighbor, neighbor2))
            val edges = network.getEdges
            edges.size() shouldBe 0
          }
        }
      }

      "takeover by nodes" in new NetworkWithOrigin {
        val network2 = new TestableNetwork(testableNetworkManager.getNextID, TestableNetworkModule)
        network2.register()
        val network3 = new TestableNetwork(testableNetworkManager.getNextID, TestableNetworkModule)
        network3.register()
        val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
        val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
        val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
        val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
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

      "takeover by loc" in new NetworkWithOrigin {
        val network2 = new TestableNetwork(testableNetworkManager.getNextID, TestableNetworkModule)
        network2.register()
        val network3 = new TestableNetwork(testableNetworkManager.getNextID, TestableNetworkModule)
        network3.register()
        val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
        val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
        val neighbor2   = new TestNode(Loc4(2, 0, 0, 0))
        val neighbor2TE = new TestableTileEntity(Loc4(2, 0, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(2, 0, 0, 0).getPos, neighbor2TE)
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
        edges should contain(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0))

        network.addConnection(neighbor.getLoc, neighbor2.getLoc)

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
        val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
        val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
        val neighbor2   = new TestNode(Loc4(0, 1, 0, 0))
        val neighbor2TE = new TestableTileEntity(Loc4(0, 1, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(0, 1, 0, 0).getPos, neighbor2TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val nodes = network.getNodes
        nodes.size() shouldBe 3
        nodes should contain allOf(origin, neighbor, neighbor2)
      }

      "have 2 edges" in new NetworkWithOrigin {
        val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
        val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
        val neighbor2   = new TestNode(Loc4(0, 1, 0, 0))
        val neighbor2TE = new TestableTileEntity(Loc4(0, 1, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(0, 1, 0, 0).getPos, neighbor2TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        val edges = network.getEdges
        edges.size() shouldBe 2
        edges should contain allOf(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0), Loc4(0, 0, 0, 0) -> Loc4(0, 1, 0, 0))
      }
    }
    "adding three connectable nodes in square" should {
      "have 4 nodes" in new NetworkWithOrigin {
        val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
        val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
        val neighbor2   = new TestNode(Loc4(0, 1, 0, 0))
        val neighbor2TE = new TestableTileEntity(Loc4(0, 1, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(0, 1, 0, 0).getPos, neighbor2TE)
        val neighbor3   = new TestNode(Loc4(1, 1, 0, 0))
        val neighbor3TE = new TestableTileEntity(Loc4(1, 1, 0, 0), testableWorld, neighbor3, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 1, 0, 0).getPos, neighbor3TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        network.addNode(neighbor3)
        val nodes = network.getNodes
        nodes.size() shouldBe 4
        nodes should contain allOf(origin, neighbor, neighbor2, neighbor3)
      }

      "have 4 edges" in new NetworkWithOrigin {
        val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
        val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
        val neighbor2   = new TestNode(Loc4(0, 1, 0, 0))
        val neighbor2TE = new TestableTileEntity(Loc4(0, 1, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(0, 1, 0, 0).getPos, neighbor2TE)
        val neighbor3   = new TestNode(Loc4(1, 1, 0, 0))
        val neighbor3TE = new TestableTileEntity(Loc4(1, 1, 0, 0), testableWorld, neighbor3, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 1, 0, 0).getPos, neighbor3TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        network.addNode(neighbor3)
        val edges = network.getEdges
        edges.size() shouldBe 4
        edges should contain allOf(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0), Loc4(0, 0, 0, 0) -> Loc4(0, 1, 0, 0), Loc4(1, 0, 0, 0) -> Loc4(1, 1, 0, 0),
          Loc4(0, 1, 0, 0) -> Loc4(1, 1, 0, 0))
      }

      "when 2 connections removed should split" in new NetworkWithOrigin {
        val neighbor   = new TestNode(Loc4(1, 0, 0, 0))
        val neighborTE = new TestableTileEntity(Loc4(1, 0, 0, 0), testableWorld, neighbor, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 0, 0, 0).getPos, neighborTE)
        val neighbor2   = new TestNode(Loc4(0, 1, 0, 0))
        val neighbor2TE = new TestableTileEntity(Loc4(0, 1, 0, 0), testableWorld, neighbor2, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(0, 1, 0, 0).getPos, neighbor2TE)
        val neighbor3   = new TestNode(Loc4(1, 1, 0, 0))
        val neighbor3TE = new TestableTileEntity(Loc4(1, 1, 0, 0), testableWorld, neighbor3, TestableNetworkModule)
        testableWorld.setITileEntity(Loc4(1, 1, 0, 0).getPos, neighbor3TE)
        network.addNode(neighbor)
        network.addNode(neighbor2)
        network.addNode(neighbor3)
        var nodes = network.getNodes
        nodes.size() shouldBe 4
        nodes should contain allOf(origin, neighbor, neighbor2, neighbor3)
        var edges = network.getEdges
        edges.size() shouldBe 4
        edges should contain allOf(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0), Loc4(0, 0, 0, 0) -> Loc4(0, 1, 0, 0), Loc4(1, 0, 0, 0) -> Loc4(1, 1, 0, 0),
          Loc4(0, 1, 0, 0) -> Loc4(1, 1, 0, 0))

        network.removeConnection(Loc4(0, 0, 0, 0), Loc4(0, 1, 0, 0))
        nodes = network.getNodes
        nodes.size() shouldBe 4
        nodes should contain allOf(origin, neighbor, neighbor2, neighbor3)
        edges = network.getEdges
        edges.size() shouldBe 3
        edges should contain allOf(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0), Loc4(1, 0, 0, 0) -> Loc4(1, 1, 0, 0), Loc4(0, 1, 0, 0) -> Loc4(1, 1, 0, 0))

        network.removeConnection(Loc4(1, 0, 0, 0), Loc4(1, 1, 0, 0))
        origin.network should be theSameInstanceAs neighbor.network
        val newNetwork = origin.getNetwork
        nodes = newNetwork.getNodes
        nodes.size() shouldBe 2
        nodes should contain allOf(origin, neighbor)
        edges = newNetwork.getEdges
        edges.size() shouldBe 1
        edges should contain(Loc4(0, 0, 0, 0) -> Loc4(1, 0, 0, 0))

        neighbor2.network should be theSameInstanceAs neighbor3.network
        val network2 = neighbor2.network
        nodes = network2.getNodes
        nodes.size() shouldBe 2
        nodes should contain allOf(neighbor2, neighbor3)
        edges = network2.getEdges
        edges.size() shouldBe 1
        edges should contain(Loc4(0, 1, 0, 0) -> Loc4(1, 1, 0, 0))

        newNetwork shouldNot be theSameInstanceAs network2

        testableNetworkManager.networks.size() shouldBe 2
        testableNetworkManager.getNetwork(network.id) shouldBe None
      }
    }
  }


  /*
  TEST CLASS EXTENSIONS
   */

  class TestableNetwork(_id: Int, val mod: IModule[TestNode]) extends TileNetwork[TestNode, TestableNetwork](_id) {

    override def networkModule: IModule[TestNode] = mod

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
    override def create(): TestableNetwork = new TestableNetwork(ManagerNetwork.instance.getNextID, mod)
  }

  class TestNode(val loc: Loc4) extends TileNetworkNode[TestNode, TestableNetwork] {
    override def getLoc: Loc4 = loc
  }

  class TestableTileEntity(val loc: Loc4, val world: IWorld, val testNode: TestNode, val module: IModule[TestNode]) extends ITileEntity {

    override def toMinecraft: TileEntity = null

    override def getPos: BlockPos = loc.getPos

    override def getIWorld: IWorld = world

    override def getBlockIdentifier: BlockIdentifier = BlockIdentifier("Test", "TestableTileEntityContainer")

    override def markDirtyForSave(): Unit = {}

    override def hasIWorld: Boolean = true

    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = false

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = null.asInstanceOf[T]

    override def hasModule(mod: IModule[_], facing: EnumFacing): Boolean = mod == module

    override def getModule[T](mod: IModule[T], facing: EnumFacing): T =
      if (mod == module) testNode.asInstanceOf[T] else null.asInstanceOf[T]
  }

}
