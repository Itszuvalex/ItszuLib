package com.itszuvalex.itszulib.core.frag

import com.itszuvalex.itszulib.TestIO
import com.itszuvalex.itszulib.TestableCoreBlockEntity
import com.itszuvalex.itszulib.TestableLevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.adapters.Module
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.core.INetworkManager
import com.itszuvalex.itszulib.core.NetworkManager
import com.itszuvalex.itszulib.core.TileNetwork
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Blocks
import net.neoforged.fml.LogicalSide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FragConnectableTest {
    private fun attached(): Pair<TestableCoreBlockEntity, FragConnectable> {
        val be = TestableCoreBlockEntity(BlockPos.ZERO)
        val frag = FragConnectable()
        be.fragList.addInternalFragment(frag)
        return be to frag
    }

    @Test
    fun ConnectDisconnect_ChangesSaveAndSync() {
        val (be, frag) = attached()
        assertTrue(frag.connect(Direction.UP))
        assertTrue(frag.isConnected(Direction.UP))
        assertTrue(frag.connect(Direction.UP))
        assertEquals(1, be.syncCount, "connecting an already-connected face changed nothing")
        assertTrue(frag.disconnect(Direction.UP))
        assertFalse(frag.isConnected(Direction.UP))
        assertEquals(2, be.syncCount)
    }

    @Test
    fun NullFace_NeverConnectsAndCountsAsBlocked() {
        val (_, frag) = attached()
        assertFalse(frag.connect(null))
        assertFalse(frag.isConnected(null))
        assertTrue(frag.isBlocked(null))
        assertFalse(frag.block(null))
        assertFalse(frag.unblock(null))
    }

    @Test
    fun Block_DisconnectsAndRefusesConnections() {
        val (_, frag) = attached()
        frag.connect(Direction.NORTH)
        assertTrue(frag.block(Direction.NORTH), "reports it was connected")
        assertFalse(frag.isConnected(Direction.NORTH))
        assertFalse(frag.connect(Direction.NORTH))
        assertFalse(frag.disconnect(Direction.NORTH))
        assertTrue(frag.unblock(Direction.NORTH))
        assertTrue(frag.connect(Direction.NORTH))
    }

    @Test
    fun Serialization_LevelAndDescription_RoundTrip() {
        val (_, frag) = attached()
        frag.connect(Direction.EAST)
        frag.block(Direction.DOWN)
        assertFalse(frag.handlesScope(NBTSerializationScope.ITEM))
        val tag = TestIO.write { frag.serializeTo(NBTSerializationScope.DESCRIPTION, it) }
        val copy = FragConnectable()
        copy.deserialize(TestIO.read(tag), NBTSerializationScope.DESCRIPTION)
        assertTrue(copy.isConnected(Direction.EAST))
        assertTrue(copy.isBlocked(Direction.DOWN))
        assertFalse(copy.isBlocked(Direction.UP))
    }
}

class TestWireNetwork(id: Int, private val manager: INetworkManager) : TileNetwork<TestWire, TestWireNetwork>(id, LogicalSide.SERVER) {
    override fun create(): TestWireNetwork = TestWireNetwork(manager.getNextID(), manager)
    override fun networkModule(): IModule<TestWire> = TestWire.MODULE
    override fun register() = manager.addNetwork(this)
    override fun unregister() = manager.removeNetwork(this)
}

class TestWire(manager: INetworkManager) : FragNetworkedWire<TestWire, TestWireNetwork>({ TestWireNetwork(manager.getNextID(), manager) }) {
    /**
     * Faces this wire refuses to connect on.
     */
    val refuse = HashSet<Direction>()

    override fun module(): IModule<TestWire> = MODULE

    override fun shouldConnect(face: Direction, other: TestWire): Boolean = face !in refuse

    companion object {
        val MODULE: IModule<TestWire> by lazy { Module.registerModule(Identifier.fromNamespaceAndPath("itszulib_test", "wire"), null) }
    }
}

class FragNetworkedWireTest {
    private val stone = Blocks.STONE.defaultBlockState()
    private lateinit var level: TestableLevel
    private lateinit var manager: NetworkManager

    @BeforeEach
    fun setup() {
        level = TestableLevel()
        manager = NetworkManager()
    }

    private fun wire(pos: BlockPos, load: Boolean = true): TestWire {
        val be = TestableCoreBlockEntity(pos, level)
        val wire = TestWire(manager)
        be.fragList.addFragment(wire)
        if (load) wire.onLoad(level, pos)
        return wire
    }

    private fun at(x: Int) = BlockPos(x, 0, 0)

    private fun breakWire(wire: TestWire, pos: BlockPos) {
        wire.onRemove(level, pos, stone)
        wire.invalidateFrags()
        level.removeIBlockEntity(pos)
        Direction.entries.forEach { face ->
            (level.getIBlockEntity(pos.relative(face)) as? TestableCoreBlockEntity)?.fragList?.onNeighborChanged(level, pos.relative(face))
        }
    }

    @Test
    fun Load_LoneWire_GetsItsOwnNetwork() {
        val a = wire(at(0))
        assertNotNull(a.getNetwork())
        assertEquals(1, a.getNetwork()!!.size())
        assertEquals(1, manager.networkCount())
    }

    @Test
    fun Load_NextToAnotherWire_ConnectsBothSidesInOneNetwork() {
        val a = wire(at(0))
        val b = wire(at(1))
        assertTrue(a.isConnected(Direction.EAST))
        assertTrue(b.isConnected(Direction.WEST))
        assertSame(a.getNetwork(), b.getNetwork())
        assertEquals(2, a.getNetwork()!!.size())
        assertEquals(1, manager.networkCount())
    }

    @Test
    fun BreakMiddleOfThree_SplitsAndClearsTheNeighboursFlags() {
        val a = wire(at(0))
        val b = wire(at(1))
        val c = wire(at(2))
        assertEquals(3, a.getNetwork()!!.size())
        breakWire(b, at(1))
        assertFalse(a.isConnected(Direction.EAST))
        assertFalse(c.isConnected(Direction.WEST))
        assertNotSame(a.getNetwork(), c.getNetwork())
        assertEquals(1, a.getNetwork()!!.size())
        assertEquals(1, c.getNetwork()!!.size())
        assertNull(b.getNetwork())
    }

    /**
     * Regression: 1.12.2's `removePersistedConnection` cleared only its own flag, so the neighbour reconnected the next
     * time it loaded.
     */
    @Test
    fun RemovePersistedConnection_ClearsBothSides() {
        val a = wire(at(0))
        val b = wire(at(1))
        a.removePersistedConnection(b.getLoc())
        assertFalse(a.isConnected(Direction.EAST))
        assertFalse(b.isConnected(Direction.WEST))
        assertNotSame(a.getNetwork(), b.getNetwork())
    }

    @Test
    fun Block_SplitsAndStaysApart_UnblockReconnects() {
        val a = wire(at(0))
        val b = wire(at(1))
        assertTrue(a.block(Direction.EAST))
        assertFalse(b.isConnected(Direction.WEST))
        assertNotSame(a.getNetwork(), b.getNetwork())
        b.onNeighborChanged(level, at(1))
        assertFalse(b.isConnected(Direction.WEST), "a neighbour's blocked face must not reconnect")
        a.unblock(Direction.EAST)
        assertTrue(a.isConnected(Direction.EAST) && b.isConnected(Direction.WEST))
        assertSame(a.getNetwork(), b.getNetwork())
    }

    @Test
    fun ShouldConnectFalse_OnEitherSide_NoConnection() {
        val a = wire(at(0), load = false)
        a.refuse += Direction.EAST
        a.onLoad(level, at(0))
        val b = wire(at(1))
        assertFalse(a.isConnected(Direction.EAST))
        assertFalse(b.isConnected(Direction.WEST))
    }

    /**
     * Regression: a connected flag towards a neighbour that is gone (removed while this chunk was unloaded) used to
     * survive loading; faces are now reconciled on load.
     */
    @Test
    fun Load_StaleFlagTowardsMissingNeighbour_Cleared() {
        val a = wire(at(0), load = false)
        a.connections.set(Direction.EAST)
        a.onLoad(level, at(0))
        assertFalse(a.isConnected(Direction.EAST))
    }

    /**
     * Neighbours in unloaded chunks are left alone (never looked up, flag kept), and joined once they load.
     */
    @Test
    fun NeighbourUnloaded_FlagKeptThenJoinedOnLoad() {
        val a = wire(at(0), load = false)
        a.connections.set(Direction.EAST)
        level.unloaded += at(1)
        a.onLoad(level, at(0))
        assertTrue(a.isConnected(Direction.EAST))

        level.unloaded -= at(1)
        val b = wire(at(1), load = false)
        b.connections.set(Direction.WEST)
        b.onLoad(level, at(1))
        assertSame(a.getNetwork(), b.getNetwork())
        assertEquals(2, a.getNetwork()!!.size())
    }

    @Test
    fun ChunkUnload_LeavesTheNetwork() {
        val a = wire(at(0))
        val b = wire(at(1))
        b.invalidateFrags()
        assertNull(b.getNetwork())
        assertEquals(1, a.getNetwork()!!.size())
        assertTrue(a.isConnected(Direction.EAST), "unloading is not breaking: the flag stays for the next load")
    }

    @Test
    fun ClientSide_DoesNothing() {
        level.clientSide = true
        val a = wire(at(0))
        assertNull(a.getNetwork())
        assertEquals(0, manager.networkCount())
    }

    @Test
    fun CanConnect_OnlyUnblockedNeighbours() {
        val a = wire(at(0))
        val b = wire(at(1))
        val far = wire(at(5))
        assertTrue(a.canConnect(b.getLoc()))
        assertFalse(a.canConnect(far.getLoc()))
        a.block(Direction.EAST)
        assertFalse(a.canConnect(b.getLoc()))
    }
}
