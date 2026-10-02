package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.MCAssert
import com.itszuvalex.itszulib.TestableLevel
import com.itszuvalex.itszulib.TestableLoc4
import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.adapters.Module
import com.itszuvalex.itszulib.api.utility.ChunkCoord
import com.itszuvalex.itszulib.api.utility.IMutableModuleCapabilityMap
import com.itszuvalex.itszulib.api.utility.Loc4
import com.itszuvalex.itszulib.api.utility.Loc4Indirect
import com.itszuvalex.itszulib.api.utility.ModuleCapabilityArrayListMap
import com.itszuvalex.itszulib.core.frag.BlockEntityFragment
import com.itszuvalex.itszulib.core.frag.FragColorable
import com.itszuvalex.itszulib.core.frag.InternalBlockEntityFragment
import com.itszuvalex.itszulib.util.Color
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.fml.LogicalSide
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.random.Random

class TestableNetworkNodeBlockEntity(private val pos: BlockPos, private val level: ILevel) : IBlockEntity {
    val moduleCapabilityMap: IMutableModuleCapabilityMap = ModuleCapabilityArrayListMap()
    override fun getBlockPos(): BlockPos = pos
    override fun toMinecraft(): BlockEntity = MCAssert.failVanillaClass("toMinecraft")
    override fun <T : Any> getModule(module: IModule<T>, side: Direction?): T? = moduleCapabilityMap.getModule(module, side)
}

/**
 * Records dirty/sync requests from fragments.
 */
class TestableFragmentHost : IFragmentHost {
    val blockEntity: IBlockEntity = TestableNetworkNodeBlockEntity(BlockPos.ZERO, TestableLevel(TestableLoc4.DEFAULT_DIM))
    var dirtyCount = 0
    var syncCount = 0
    override fun blockEntity(): IBlockEntity = blockEntity
    override fun markDirty() {
        dirtyCount++
    }
    override fun markDirtyAndSync() {
        dirtyCount++
        syncCount++
    }
}

class TestableNetwork(id: Int, private val module: IModule<TestableNetworkNode>, private val manager: INetworkManager) :
    TileNetwork<TestableNetworkNode, TestableNetwork>(id, LogicalSide.SERVER) {
    /**
     * The network that took this one over, via onTakeover.
     */
    var takenOverBy: TestableNetwork? = null

    override fun create(): TestableNetwork = TestableNetwork(manager.getNextID(), module, manager)
    override fun networkModule(): IModule<TestableNetworkNode> = module
    override fun register() = manager.addNetwork(this)
    override fun unregister() = manager.removeNetwork(this)
    override fun onTakeover(network: TestableNetwork) {
        takenOverBy = network
    }
}

class TestableNetworkNode(private val loc: Loc4) : TileNetworkNode<TestableNetworkNode, TestableNetwork>() {
    /**
     * Locations this node has been told it is connected to, via onConnect/onDisconnect.
     */
    val connectedTo = HashSet<Loc4>()

    /**
     * Networks this node was removed from, in order, via onRemoved.
     */
    val removedFrom = ArrayList<TestableNetwork>()

    override fun getLoc(): Loc4 = loc
    override fun onConnect(loc: Loc4) {
        connectedTo.add(loc)
    }
    override fun onDisconnect(loc: Loc4) {
        connectedTo.remove(loc)
    }
    override fun onRemoved(network: TestableNetwork) {
        removedFrom.add(network)
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlockEntityFragmentCollectionTest {
    lateinit var module: IModule<String>

    @BeforeAll
    fun classSetup() {
        module = Module.registerModule(Identifier.fromNamespaceAndPath("itszulib_test", "fragment_collection"), null)
    }

    @AfterAll
    fun classTeardown() = Module.clear()

    private fun collection() = BlockEntityFragmentCollection(TestableFragmentHost())

    private fun internal(name: String) = object : InternalBlockEntityFragment() {
        override fun name(): String = name
    }

    private fun exposing(name: String, value: String) = object : BlockEntityFragment<String>() {
        override fun name(): String = name
        override fun module(): IModule<String> = this@BlockEntityFragmentCollectionTest.module
        override fun faceToModuleMapper(be: IBlockEntity): (Direction?) -> String? = { value }
    }

    private class Dirtying : InternalBlockEntityFragment() {
        override fun name(): String = "Dirtying"
        fun change() {
            markDirty()
            markDirtyAndSync()
        }
    }

    @Test
    fun AddInternalFragment_AttachesHost() {
        val host = TestableFragmentHost()
        val frags = BlockEntityFragmentCollection(host)
        val frag = Dirtying()
        frag.change() // Not attached yet: no-op.
        Assertions.assertEquals(0, host.dirtyCount)
        frags.addInternalFragment(frag)
        frag.change()
        Assertions.assertEquals(2, host.dirtyCount)
        Assertions.assertEquals(1, host.syncCount)
    }

    @Test
    fun AddInternalFragment_DuplicateName_Throws() {
        val frags = collection()
        frags.addInternalFragment(internal("Inventory"))
        Assertions.assertThrows(IllegalArgumentException::class.java) { frags.addInternalFragment(internal("Inventory")) }
    }

    @Test
    fun AddFragment_ModuleAlreadyExposed_Throws() {
        val frags = collection()
        frags.addFragment(exposing("First", "a"))
        Assertions.assertThrows(IllegalArgumentException::class.java) { frags.addFragment(exposing("Second", "b")) }
        Assertions.assertEquals("a", frags.getModule(module, null))
    }

    @Test
    fun InvalidateFrags_HidesModulesUntilRehydrated() {
        val frags = collection()
        frags.addFragment(exposing("First", "a"))
        frags.invalidateFrags()
        Assertions.assertNull(frags.getModule(module, null))
        frags.rehydrateFrags()
        Assertions.assertEquals("a", frags.getModule(module, null))
    }
}

class FragColorableTest {
    @Test
    fun SetColor_Changed_MarksDirtyAndSyncs() {
        val host = TestableFragmentHost()
        val frag = FragColorable()
        frag.onAttach(host)
        frag.setColor(Color(0xFF00FF00.toInt()))
        Assertions.assertEquals(Color(0xFF00FF00.toInt()), frag.getColor())
        Assertions.assertEquals(1, host.syncCount)
    }

    @Test
    fun SetColor_Unchanged_DoesNothing() {
        val host = TestableFragmentHost()
        val frag = FragColorable(Color(0xFF00FF00.toInt()))
        frag.onAttach(host)
        frag.setColor(Color(0xFF00FF00.toInt()))
        Assertions.assertEquals(0, host.dirtyCount)
    }
}

class ColorTest {
    @Test
    fun IntRoundTrip_PreservesAllChannels() {
        for (argb in intArrayOf(0, 0xFF102030.toInt(), 0x80FFFFFF.toInt(), 0x7F000001, -1)) {
            Assertions.assertEquals(argb, Color(argb).toInt())
        }
        val c = Color(0xFF102030.toInt())
        Assertions.assertEquals(0xFF.toByte(), c.alpha)
        Assertions.assertEquals(0x10.toByte(), c.red)
        Assertions.assertEquals(0x20.toByte(), c.green)
        Assertions.assertEquals(0x30.toByte(), c.blue)
    }

    @Test
    fun With_ReturnsChangedCopyLeavesOriginal() {
        val original = Color(0x01020304)
        val changed = original.withRed(9)
        Assertions.assertEquals(0x01020304, original.toInt())
        Assertions.assertEquals(0x01090304, changed.toInt())
        Assertions.assertEquals(Color(0x01090304), changed)
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NetworkTest {
    lateinit var module: IModule<TestableNetworkNode>
    val networkManager: INetworkManager = NetworkManager()
    lateinit var dimension: Identifier

    @BeforeAll
    fun classSetup() {
        module = Module.registerModule(Identifier.fromNamespaceAndPath("itszulib_test", "network"), null)
    }

    @AfterAll
    fun classTeardown() {
        Module.clear()
        networkManager.clear()
    }

    @BeforeEach
    fun methodSetup() {
        dimension = Identifier.parse(Random.nextInt(0, Int.MAX_VALUE).toString())
    }

    @AfterEach
    fun methodTeardown() = networkManager.clear()

    inner class TestState {
        val network = TestableNetwork(networkManager.getNextID(), module, networkManager).also { it.register() }
        val level = TestableLevel(dimension)

        fun createNode(pos: BlockPos): TestableNetworkNode {
            val e = TestableNetworkNodeBlockEntity(pos, level)
            val node = TestableNetworkNode(Loc4.of(level, pos))
            e.moduleCapabilityMap.addModule(module) { node }
            level.setIBlockEntity(e)
            return node
        }

        fun newNetwork() = TestableNetwork(networkManager.getNextID(), module, networkManager).also { it.register() }
    }

    private fun line(state: TestState, n: Int) = (0 until n).map { x ->
        state.createNode(BlockPos(x, 0, 0)).also { state.network.addNode(it) }
    }

    @Test
    fun Construct_Empty() {
        val state = TestState()
        Assertions.assertEquals(0, state.network.size())
        Assertions.assertEquals(0, state.network.getNodes().count())
        Assertions.assertEquals(0, state.network.getConnections().size)
        Assertions.assertEquals(0, state.network.getEdges().count())
    }

    @Test
    fun AddNode_AddAndContains() {
        val state = TestState()
        val node = state.createNode(BlockPos(0, 0, 0))
        state.network.addNode(node)
        Assertions.assertEquals(1, state.network.size())
        Assertions.assertTrue(state.network.getNodes().any { it == node })
    }

    @Test
    fun AddNode_Connectable_AddContainAndAddEdge() {
        val state = TestState()
        val (node, neighbor) = line(state, 2)
        Assertions.assertEquals(2, state.network.size())
        Assertions.assertEquals(1, state.network.getEdges().count())
        val map = state.network.getConnections()
        Assertions.assertEquals(2, map.size)
        Assertions.assertTrue(neighbor.getLoc() in map[node.getLoc()]!!)
        Assertions.assertTrue(node.getLoc() in map[neighbor.getLoc()]!!)
        Assertions.assertEquals(listOf(NetworkEdge(node.getLoc(), neighbor.getLoc())), state.network.getEdges().toList())
    }

    @Test
    fun Create_ShouldCreateLikeNetworkKind() {
        Assertions.assertInstanceOf(TestableNetwork::class.java, TestState().network.create())
    }

    @Test
    fun AddNode_Connectable3InLine_AddContainAndAddEdge() {
        val state = TestState()
        val (a, b, c) = line(state, 3)
        Assertions.assertEquals(3, state.network.size())
        Assertions.assertEquals(
            setOf(NetworkEdge(a.getLoc(), b.getLoc()), NetworkEdge(b.getLoc(), c.getLoc())),
            state.network.getEdges().toSet(),
        )
    }

    @Test
    fun RemoveNode_EndOf3InLine_RemoveNodeAndEdge() {
        val state = TestState()
        val (a, b, c) = line(state, 3)
        state.network.removeNode(c)
        Assertions.assertEquals(2, state.network.size())
        Assertions.assertEquals(listOf(NetworkEdge(a.getLoc(), b.getLoc())), state.network.getEdges().toList())
        Assertions.assertFalse(state.network.getConnections().containsKey(c.getLoc()))
        Assertions.assertEquals(setOf(a.getLoc()), state.network.getConnections()[b.getLoc()])
    }

    @Test
    fun RemoveNode_MiddleOf3InLine_SplitAndMakeSubnetworks() {
        val state = TestState()
        val (a, b, c) = line(state, 3)
        state.network.removeNode(b)
        Assertions.assertEquals(0, state.network.size())
        Assertions.assertEquals(0, state.network.getEdges().count())
        Assertions.assertNull(networkManager.getNetwork(state.network.ID()))
        Assertions.assertNotSame(state.network, a.getNetwork())
        Assertions.assertNotSame(state.network, c.getNetwork())
        Assertions.assertNotSame(a.getNetwork(), c.getNetwork())
        for (n in listOf(a, c)) {
            Assertions.assertEquals(1, n.getNetwork()!!.size())
            Assertions.assertEquals(0, n.getNetwork()!!.getEdges().count())
            Assertions.assertTrue(n.getNetwork()!!.getNodes().any { it == n })
            Assertions.assertNotNull(networkManager.getNetwork(n.getNetwork()!!.ID()))
        }
    }

    @Test
    fun RemoveConnection_1From2In3InLine_SplitAndMakeSubnetworks() {
        val state = TestState()
        val (a, b, c) = line(state, 3)
        state.network.removeConnectionNodes(a, b)
        Assertions.assertEquals(0, state.network.size())
        Assertions.assertNull(networkManager.getNetwork(state.network.ID()))
        Assertions.assertEquals(1, a.getNetwork()!!.size())
        Assertions.assertEquals(2, b.getNetwork()!!.size())
        Assertions.assertEquals(1, b.getNetwork()!!.getEdges().count())
        Assertions.assertSame(b.getNetwork(), c.getNetwork())
        Assertions.assertNotNull(networkManager.getNetwork(b.getNetwork()!!.ID()))
    }

    @Test
    fun RemoveNode_MiddleOf4InLine_SplitAndMakeSubnetworks() {
        val state = TestState()
        val (a, b, c, d) = line(state, 4)
        state.network.removeNode(b)
        Assertions.assertEquals(0, state.network.size())
        Assertions.assertNotSame(a.getNetwork(), c.getNetwork())
        Assertions.assertSame(c.getNetwork(), d.getNetwork())
        Assertions.assertEquals(1, a.getNetwork()!!.size())
        Assertions.assertEquals(2, c.getNetwork()!!.size())
        Assertions.assertEquals(1, c.getNetwork()!!.getEdges().count())
    }

    @Test
    fun AddConnection_TakeoverOfOneNetworkByAnother() {
        val state = TestState()
        val network2 = state.newNetwork()
        val node = state.createNode(BlockPos(0, 0, 0))
        state.network.addNode(node)
        val neighbor = state.createNode(BlockPos(1, 0, 0))
        network2.addNode(neighbor)
        Assertions.assertEquals(1, state.network.size())
        Assertions.assertEquals(1, network2.size())
        state.network.addConnectionNodes(node, neighbor)
        Assertions.assertSame(state.network, node.getNetwork())
        Assertions.assertSame(state.network, neighbor.getNetwork())
        Assertions.assertNull(networkManager.getNetwork(network2.ID()))
        Assertions.assertEquals(2, state.network.size())
        Assertions.assertEquals(listOf(NetworkEdge(node.getLoc(), neighbor.getLoc())), state.network.getEdges().toList())
    }

    @Test
    fun AddConnectionLocs_NodesInDifferentNetworks_ConnectBothAndTakeover() {
        val state = TestState()
        val network2 = state.newNetwork()
        // Not neighbors, so addNode doesn't connect them on its own.
        val node = state.createNode(BlockPos(0, 0, 0))
        state.network.addNode(node)
        val other = state.createNode(BlockPos(5, 0, 0))
        network2.addNode(other)
        state.network.addConnectionLocs(node.getLoc(), other.getLoc())
        Assertions.assertEquals(setOf(other.getLoc()), node.connectedTo)
        Assertions.assertEquals(setOf(node.getLoc()), other.connectedTo)
        Assertions.assertSame(state.network, other.getNetwork())
        Assertions.assertNull(networkManager.getNetwork(network2.ID()))
        Assertions.assertEquals(listOf(NetworkEdge(node.getLoc(), other.getLoc())), state.network.getEdges().toList())
    }

    @Test
    fun AddConnectionNodes_NeitherNodeInThisNetwork_AbsorbBothAndNotifyTakeovers() {
        val state = TestState()
        val network2 = state.newNetwork()
        val network3 = state.newNetwork()
        val a = state.createNode(BlockPos(0, 0, 0)).also { network2.addNode(it) }
        val b = state.createNode(BlockPos(5, 0, 0)).also { network3.addNode(it) }
        state.network.addConnectionNodes(a, b)
        Assertions.assertSame(state.network, a.getNetwork())
        Assertions.assertSame(state.network, b.getNetwork())
        Assertions.assertEquals(2, state.network.size())
        Assertions.assertSame(state.network, network2.takenOverBy)
        Assertions.assertSame(state.network, network3.takenOverBy)
        Assertions.assertNull(networkManager.getNetwork(network2.ID()))
        Assertions.assertNull(networkManager.getNetwork(network3.ID()))
    }

    @Test
    fun AddNode_NodeInAnotherNetwork_MoveIt() {
        val state = TestState()
        val network2 = state.newNetwork()
        val node = state.createNode(BlockPos(0, 0, 0)).also { network2.addNode(it) }
        state.network.addNode(node)
        Assertions.assertSame(state.network, node.getNetwork())
        Assertions.assertEquals(listOf(network2), node.removedFrom)
        Assertions.assertEquals(0, network2.size())
        Assertions.assertNull(networkManager.getNetwork(network2.ID()))
        // Adding again is a no-op.
        state.network.addNode(node)
        Assertions.assertEquals(1, state.network.size())
        Assertions.assertEquals(listOf(network2), node.removedFrom)
    }

    @Test
    fun OnChunkUnload_RemoveNodesInChunkAndNotifyThem() {
        // A line across the chunk 0 / chunk 1 boundary (x 15 | 16).
        val state = TestState()
        val nodes = (14..17).map { state.createNode(BlockPos(it, 0, 0)) }
        nodes.forEach(state.network::addNode)
        Assertions.assertEquals(4, state.network.size())
        networkManager.onChunkUnload(state.level, ChunkCoord(1, 0))
        Assertions.assertEquals(2, state.network.size())
        Assertions.assertTrue(nodes[0].removedFrom.isEmpty())
        Assertions.assertTrue(nodes[1].removedFrom.isEmpty())
        Assertions.assertEquals(listOf(state.network), nodes[2].removedFrom)
        Assertions.assertEquals(listOf(state.network), nodes[3].removedFrom)
    }

    @Test
    fun Explore_VeryLongChain_NoStackOverflow() {
        val state = TestState()
        val length = 100_000
        val locs: List<Loc4> = (0 until length).map { Loc4Indirect(dimension, BlockPos(it, 0, 0)) }
        val edges = (1 until length).asSequence().map { NetworkEdge(locs[it - 1], locs[it]) }
        val network = state.network.createWithNodesAndEdges(locs.asSequence().map(::TestableNetworkNode), edges)
        Assertions.assertEquals(length, TileNetwork.NetworkExplorer.explore(locs[0], network).size)
    }
}
