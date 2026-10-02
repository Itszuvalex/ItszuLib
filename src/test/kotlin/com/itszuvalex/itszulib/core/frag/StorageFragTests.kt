package com.itszuvalex.itszulib.core.frag

import com.itszuvalex.itszulib.TestIO
import com.itszuvalex.itszulib.TestableCoreBlockEntity
import com.itszuvalex.itszulib.TestableIFluidStack
import com.itszuvalex.itszulib.TestableIItemStack
import com.itszuvalex.itszulib.TestableLevel
import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.storage.FluidStorageArray
import com.itszuvalex.itszulib.api.storage.IFluidStorage
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.storage.ItemStorageArray
import com.itszuvalex.itszulib.api.storage.ItemStorageSlice
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.core.EnumAutomaticIO
import com.itszuvalex.itszulib.core.SidedFluidStorageConfiguration
import com.itszuvalex.itszulib.core.SidedItemStorageConfiguration
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.transaction.TransactionContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * A machine-like inventory: slot 0 is "input", slot 1 "output"; every face starts on "input".
 */
private class Machine {
    val be = TestableCoreBlockEntity(BlockPos.ZERO, TestableLevel())
    val inventory = ItemStorageArray(2)
    val input = ItemStorageSlice(inventory, intArrayOf(0))
    val output = ItemStorageSlice(inventory, intArrayOf(1))
    val config = SidedItemStorageConfiguration(
        { "input" },
        mapOf("input" to input, "output" to output),
        { Direction.NORTH },
    )
    val storage = FragItemStorage(inventory)
    val sided = FragSidedConfiguration("ItemConfig", config, Modules.ITEM_STORAGE_CONFIGURABLE)
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FragItemStorageTest {
    @BeforeAll
    fun classSetup() = TestableIItemStack.overrideCodec()

    @AfterAll
    fun classTeardown() = TestableIItemStack.resetCodec()

    @Test
    fun NoConfiguration_SameStorageOnEverySide() {
        val m = Machine()
        m.be.fragList.addFragment(m.storage)
        assertSame(m.inventory, m.be.getModule(Modules.ITEM_STORAGE, null))
        Direction.entries.forEach { assertSame(m.inventory, m.be.getModule(Modules.ITEM_STORAGE, it)) }
    }

    @Test
    fun WithConfiguration_EachSideGetsItsConfiguredStorage() {
        val m = Machine()
        m.be.fragList.addFragment(m.sided)
        m.be.fragList.addFragment(m.storage)
        m.sided.update { it.cycleRelativeFacingStorageForward(Direction.EAST) }
        assertSame(m.inventory, m.be.getModule(Modules.ITEM_STORAGE, null), "no side: the whole storage")
        assertSame(m.output, m.be.getModule(Modules.ITEM_STORAGE, Direction.EAST))
        assertSame(m.input, m.be.getModule(Modules.ITEM_STORAGE, Direction.WEST))
    }

    @Test
    fun ConfigurationNamesUnknownStorage_SideExposesNothing() {
        val m = Machine()
        val config = SidedItemStorageConfiguration({ "missing" }, mapOf("input" to m.input), { Direction.NORTH })
        m.be.fragList.addFragment(FragSidedConfiguration("ItemConfig", config, Modules.ITEM_STORAGE_CONFIGURABLE))
        m.be.fragList.addFragment(m.storage)
        assertNull(m.be.getModule(Modules.ITEM_STORAGE, Direction.UP))
        assertNull(m.storage.handler(Direction.UP))
    }

    @Test
    fun Handler_OneInstancePerStorage() {
        val m = Machine()
        m.be.fragList.addFragment(m.sided)
        m.be.fragList.addFragment(m.storage)
        m.sided.update { it.cycleRelativeFacingStorageForward(Direction.EAST) }
        assertSame(m.storage.handler(Direction.UP), m.storage.handler(Direction.DOWN))
        assertNotSame(m.storage.handler(Direction.UP), m.storage.handler(Direction.EAST))
        assertNotSame(m.storage.handler(null), m.storage.handler(Direction.UP))
    }

    @Test
    fun Serialization_LevelScopeOnly_RoundTrips() {
        val m = Machine()
        m.inventory.setSlot(1, TestableIItemStack(3, 5, 0))
        assertTrue(m.storage.handlesScope(NBTSerializationScope.LEVEL))
        assertFalse(m.storage.handlesScope(NBTSerializationScope.DESCRIPTION))
        assertFalse(m.storage.handlesScope(NBTSerializationScope.ITEM))

        val tag = TestIO.write { m.storage.serializeTo(NBTSerializationScope.LEVEL, it) }
        val copy = Machine()
        copy.storage.deserialize(TestIO.read(tag), NBTSerializationScope.LEVEL)
        assertEquals(5, copy.inventory.get(1).stackSize())
    }

    @Test
    fun NotPersisted_HandlesNoScope() {
        val frag = FragItemStorage(ItemStorageArray(1), persist = false)
        NBTSerializationScope.entries.forEach { assertFalse(frag.handlesScope(it)) }
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FragFluidStorageTest {
    @BeforeAll
    fun classSetup() = TestableIFluidStack.overrideCodec()

    @AfterAll
    fun classTeardown() = TestableIFluidStack.resetCodec()

    @Test
    fun WithConfiguration_EachSideGetsItsConfiguredTank() {
        val be = TestableCoreBlockEntity(BlockPos.ZERO)
        val tanks = FluidStorageArray(2, 1000)
        val a = com.itszuvalex.itszulib.api.storage.FluidStorageSlice(tanks, intArrayOf(0))
        val b = com.itszuvalex.itszulib.api.storage.FluidStorageSlice(tanks, intArrayOf(1))
        val config = SidedFluidStorageConfiguration({ if (it == Direction.UP) "b" else "a" }, mapOf("a" to a, "b" to b), { Direction.NORTH })
        be.fragList.addFragment(FragSidedConfiguration("FluidConfig", config, Modules.FLUID_STORAGE_CONFIGURABLE))
        val frag = FragFluidStorage(tanks)
        be.fragList.addFragment(frag)
        assertSame(tanks, be.getModule(Modules.FLUID_STORAGE, null))
        assertSame(b, be.getModule(Modules.FLUID_STORAGE, Direction.UP))
        assertSame(a, be.getModule(Modules.FLUID_STORAGE, Direction.SOUTH))
    }

    @Test
    fun Serialization_RoundTrips() {
        val tanks = FluidStorageArray(2, 1000)
        tanks.set(1, TestableIFluidStack(4, 250))
        val tag = TestIO.write { FragFluidStorage(tanks).serializeTo(NBTSerializationScope.LEVEL, it) }
        val copy = FluidStorageArray(2, 1000)
        FragFluidStorage(copy).deserialize(TestIO.read(tag), NBTSerializationScope.LEVEL)
        assertEquals(TestableIFluidStack(4, 250), copy.get(1))
    }
}

class FragSidedConfigurationTest {
    @Test
    fun Update_AppliesChangeThenMarksDirtyAndSyncs() {
        val m = Machine()
        m.be.fragList.addFragment(m.sided)
        m.sided.update { it.cycleRelativeFacingIOForward(Direction.UP) }
        assertEquals(EnumAutomaticIO.INPUT, m.config.getIOForRelativeFacing(Direction.UP))
        assertEquals(1, m.be.syncCount)
    }

    @Test
    fun Scopes_SavedAndSyncedNotOnItem() {
        val frag = Machine().sided
        assertTrue(frag.handlesScope(NBTSerializationScope.LEVEL))
        assertTrue(frag.handlesScope(NBTSerializationScope.DESCRIPTION))
        assertFalse(frag.handlesScope(NBTSerializationScope.ITEM))
    }

    @Test
    fun ExposedAsModule_RoundTripsThroughFragmentData() {
        val m = Machine()
        m.be.fragList.addFragment(m.sided)
        m.sided.update {
            it.cycleRelativeFacingStorageForward(Direction.SOUTH)
            it.cycleRelativeFacingIOBackward(Direction.SOUTH)
        }
        assertSame(m.config, m.be.getModule(Modules.ITEM_STORAGE_CONFIGURABLE, Direction.DOWN))

        val tag = TestIO.write { m.be.fragList.serializeTo(NBTSerializationScope.DESCRIPTION, it) }
        val copy = Machine()
        copy.be.fragList.addFragment(copy.sided)
        copy.be.fragList.deserialize(TestIO.read(tag), NBTSerializationScope.DESCRIPTION)
        assertEquals("output", copy.config.getStorageNameForRelativeFacing(Direction.SOUTH))
        assertEquals(EnumAutomaticIO.OUTPUT, copy.config.getIOForRelativeFacing(Direction.SOUTH))
    }
}

/**
 * Tests FragAutoIO's scheduling and routing with stand-in handlers; real transfers are covered by the game tests.
 */
class FragAutoIOTest {
    /**
     * A handler that is only ever compared by identity.
     */
    private class Marker(val label: String) : ResourceHandler<FluidResource> {
        override fun size(): Int = error("unused")
        override fun getResource(index: Int): FluidResource = error("unused")
        override fun getAmountAsLong(index: Int): Long = error("unused")
        override fun getCapacityAsLong(index: Int, resource: FluidResource): Long = error("unused")
        override fun isValid(index: Int, resource: FluidResource): Boolean = error("unused")
        override fun insert(index: Int, resource: FluidResource, amount: Int, transaction: TransactionContext): Int = error("unused")
        override fun extract(index: Int, resource: FluidResource, amount: Int, transaction: TransactionContext): Int = error("unused")
        override fun toString(): String = label
    }

    private data class Move(val from: String, val to: String, val amount: Int)

    /**
     * Moves report [moved] of what they were asked for; neighbours are markers named after their direction.
     */
    private class RecordingAutoIO(
        configModule: IModule<SidedFluidStorageConfiguration>,
        ticks: Int,
        private val amount: Int,
        private val moved: (Int) -> Int = { it },
        private val stubNeighbours: Boolean = true,
    ) : FragAutoIO<IFluidStorage, FluidResource>("AutoIO", configModule, net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, { ticks }, { amount }) {
        val moves = ArrayList<Move>()
        val wrapped = ArrayList<IFluidStorage>()

        override fun wrap(storage: IFluidStorage): ResourceHandler<FluidResource> {
            wrapped += storage
            return Marker("ours${wrapped.size}")
        }

        override fun move(from: ResourceHandler<FluidResource>, to: ResourceHandler<FluidResource>, amount: Int): Int {
            moves += Move(from.toString(), to.toString(), amount)
            return moved(amount)
        }

        override fun neighbour(level: ILevel, pos: BlockPos, side: Direction): ResourceHandler<FluidResource>? =
            if (stubNeighbours) Marker(side.opposite.name) else super.neighbour(level, pos, side)
    }

    private val tank = FluidStorageArray(1, 1000)

    private fun setup(io: Map<Direction, EnumAutomaticIO>, autoIO: (IModule<SidedFluidStorageConfiguration>) -> RecordingAutoIO): Pair<TestableCoreBlockEntity, RecordingAutoIO> {
        val be = TestableCoreBlockEntity(BlockPos.ZERO)
        val config = SidedFluidStorageConfiguration({ "tank" }, mapOf("tank" to tank), { Direction.NORTH })
        io.forEach { (face, mode) -> repeat(mode.ordinal) { config.cycleRelativeFacingIOForward(face) } }
        be.fragList.addFragment(FragSidedConfiguration("FluidConfig", config, Modules.FLUID_STORAGE_CONFIGURABLE))
        val frag = autoIO(Modules.FLUID_STORAGE_CONFIGURABLE)
        be.fragList.addInternalFragment(frag)
        return be to frag
    }

    private fun tick(be: TestableCoreBlockEntity, frag: FragAutoIO<*, *>) = frag.tick(be.level, BlockPos.ZERO, Blocks.STONE.defaultBlockState())

    @Test
    fun IncrementTicks_CountsDownAndWraps() {
        assertEquals(19, FragAutoIO.incrementTicks(0, 20))
        assertEquals(0, FragAutoIO.incrementTicks(1, 20))
        assertEquals(0, FragAutoIO.incrementTicks(5, 0))
        assertEquals(0, FragAutoIO.incrementTicks(0, 1))
    }

    @Test
    fun Tick_PullsOnInputFacesPushesOnOutputFaces() {
        val (be, frag) = setup(mapOf(Direction.WEST to EnumAutomaticIO.INPUT, Direction.EAST to EnumAutomaticIO.OUTPUT)) {
            RecordingAutoIO(it, 1, 100)
        }
        tick(be, frag)
        assertEquals(listOf(Move("WEST", "ours1", 100), Move("ours1", "EAST", 100)), frag.moves)
        assertEquals(1, be.dirtyCount)
    }

    @Test
    fun Tick_BudgetIsSharedAcrossFaces() {
        val (be, frag) = setup(mapOf(Direction.WEST to EnumAutomaticIO.INPUT, Direction.EAST to EnumAutomaticIO.INPUT, Direction.UP to EnumAutomaticIO.INPUT)) {
            RecordingAutoIO(it, 1, 100, moved = { 60 })
        }
        tick(be, frag)
        // The first face takes 60 of 100; the second may take the remaining 40; the third gets nothing.
        assertEquals(listOf(100, 40), frag.moves.map { it.amount })
    }

    @Test
    fun Tick_OnlyEveryTicksPerOperation() {
        val (be, frag) = setup(mapOf(Direction.WEST to EnumAutomaticIO.INPUT)) { RecordingAutoIO(it, 3, 10) }
        repeat(6) { tick(be, frag) }
        assertEquals(2, frag.moves.size)
    }

    @Test
    fun Tick_ClientSide_DoesNothing() {
        val (be, frag) = setup(mapOf(Direction.WEST to EnumAutomaticIO.INPUT)) { RecordingAutoIO(it, 1, 10) }
        be.level.clientSide = true
        tick(be, frag)
        assertTrue(frag.moves.isEmpty())
    }

    @Test
    fun Tick_NothingMoved_DoesNotMarkDirty() {
        val (be, frag) = setup(mapOf(Direction.WEST to EnumAutomaticIO.INPUT)) { RecordingAutoIO(it, 1, 10, moved = { 0 }) }
        tick(be, frag)
        assertEquals(0, be.dirtyCount)
    }

    @Test
    fun Handler_WrapsEachStorageOnce() {
        val (be, frag) = setup(mapOf(Direction.WEST to EnumAutomaticIO.INPUT, Direction.EAST to EnumAutomaticIO.OUTPUT)) {
            RecordingAutoIO(it, 1, 10)
        }
        repeat(3) { tick(be, frag) }
        assertEquals(1, frag.wrapped.size)
    }

    /**
     * Regression: auto IO looked neighbours up through `Level#getCapability`, which loads an unloaded neighbour's
     * chunk on the server.
     */
    @Test
    fun Tick_NeighbourUnloaded_NotLookedUp() {
        val (be, frag) = setup(mapOf(Direction.WEST to EnumAutomaticIO.INPUT)) {
            RecordingAutoIO(it, 1, 10, stubNeighbours = false)
        }
        be.level.unloaded += BlockPos.ZERO.relative(Direction.WEST)
        // TestableLevel#toMinecraft fails the test if reached.
        tick(be, frag)
        assertTrue(frag.moves.isEmpty())
    }

    @Test
    fun Serialization_SavesTicks() {
        val (be, frag) = setup(mapOf()) { RecordingAutoIO(it, 5, 10) }
        tick(be, frag)
        val tag = TestIO.write { frag.serializeTo(NBTSerializationScope.LEVEL, it) }
        val (_, copy) = setup(mapOf()) { RecordingAutoIO(it, 5, 10) }
        copy.deserialize(TestIO.read(tag), NBTSerializationScope.LEVEL)
        assertEquals(frag.ticks, copy.ticks)
        assertNotNull(frag.ticks)
    }
}
