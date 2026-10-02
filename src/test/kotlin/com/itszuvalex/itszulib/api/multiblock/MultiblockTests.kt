package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.TestIO
import com.itszuvalex.itszulib.TestableCoreBlockEntity
import com.itszuvalex.itszulib.TestableLevel
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.storage.FluidStorageArray
import com.itszuvalex.itszulib.api.storage.IFluidStorage
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.storage.ItemStorageArray
import com.itszuvalex.itszulib.api.utility.ChunkCoord
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.core.EnumAutomaticIO
import com.itszuvalex.itszulib.core.SidedStorageConfiguration
import com.itszuvalex.itszulib.core.frag.FragMultiblockPart
import com.itszuvalex.itszulib.core.frag.FragMultiblockTickable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Shared state for tests: a counter that records [onBreak].
 */
class TestCounter(private val onChanged: Runnable) : IMultiblockState {
    var count = 0
        set(value) {
            field = value
            onChanged.run()
        }

    val breaks = ArrayList<Pair<BlockPos, BlockPos>>()

    override fun onBreak(level: ILevel, anchor: BlockPos, brokenAt: BlockPos) {
        breaks += anchor to brokenAt
    }

    override fun serialize(output: ValueOutput) = output.putInt("count", count)

    override fun deserialize(input: ValueInput) {
        count = input.getIntOr("count", 0)
    }
}

/**
 * Records ticket refreshes; [ticking] decides which positions tick.
 */
class TestableTickets : IChunkTickets {
    val refreshed = ArrayList<ChunkCoord>()
    var ticking: (BlockPos) -> Boolean = { true }

    override fun refresh(level: ILevel, chunk: ChunkCoord) {
        refreshed += chunk
    }

    override fun isTicking(level: ILevel, pos: BlockPos): Boolean = ticking(pos)
}

/**
 * Shapes shared by the multiblock tests; registered once (shape ids are global).
 */
object TestShapes {
    lateinit var PAIR: MultiblockShape
    lateinit var TRIPLE: MultiblockShape
    lateinit var LINKED_TRIPLE: MultiblockShape
    lateinit var STATEFUL_PAIR: MultiblockShape
    lateinit var STATEFUL_LINKED_PAIR: MultiblockShape

    private var registered = false

    fun register() {
        if (registered) return
        registered = true
        PAIR = MultiblockShape.register(id("pair"), mapOf(BlockPos.ZERO to "core", BlockPos(1, 0, 0) to "wing"))
        TRIPLE = MultiblockShape.register(id("triple"), mapOf(BlockPos.ZERO to "core", BlockPos(1, 0, 0) to "wing", BlockPos(-1, 0, 0) to "wing"))
        LINKED_TRIPLE = MultiblockShape.register(
            id("linked_triple"), mapOf(BlockPos.ZERO to "core", BlockPos(1, 0, 0) to "wing", BlockPos(-1, 0, 0) to "wing"),
            MultiblockBreakPolicy.DESTROY_ALL,
        )
        STATEFUL_PAIR = MultiblockShape.register(
            id("stateful_pair"), mapOf(BlockPos.ZERO to "core", BlockPos(1, 0, 0) to "wing"), MultiblockBreakPolicy.DISSOLVE, ::TestCounter,
        )
        STATEFUL_LINKED_PAIR = MultiblockShape.register(
            id("stateful_linked_pair"), mapOf(BlockPos.ZERO to "core", BlockPos(1, 0, 0) to "wing"), MultiblockBreakPolicy.DESTROY_ALL, ::TestCounter,
        )
    }

    fun clear() {
        MultiblockShape.clear()
        registered = false
    }

    private fun id(path: String) = Identifier.fromNamespaceAndPath("itszulib_test", path)
}

/**
 * Fixtures: block entities hosting a [FragMultiblockPart] wired to a test [manager], in a [TestableLevel].
 */
abstract class MultiblockTestBase {
    protected val level = TestableLevel()
    protected val destroyed = ArrayList<BlockPos>()
    protected val tickets = TestableTickets()
    protected var manager = MultiblockManager({ _, pos -> destroyed.add(pos) }, tickets)

    protected inner class Part(val pos: BlockPos, shape: MultiblockShape, roles: List<String>, autoForm: Boolean = true) {
        val be = TestableCoreBlockEntity(pos, level)
        val part = FragMultiblockPart(roles.map { MultiblockRoleRef(shape, it) }, autoForm) { manager }

        init {
            be.fragList.addFragment(part)
        }

        val membership get() = part.membership

        fun load() = manager.onPartLoaded(level, pos, part)

        fun unload() = manager.onPartUnloaded(part)

        fun remove() = manager.onPartRemoved(level, pos, part)
    }

    protected fun place(shape: MultiblockShape, pos: BlockPos, vararg roles: String, autoForm: Boolean = true) = Part(pos, shape, roles.toList(), autoForm)

    companion object {
        @BeforeAll
        @JvmStatic
        fun classSetup() = TestShapes.register()

        @AfterAll
        @JvmStatic
        fun classTeardown() = TestShapes.clear()
    }
}

class MultiblockManagerTest : MultiblockTestBase() {
    private val pair get() = TestShapes.PAIR

    @Test
    fun OnPartLoaded_AllSlotsPresentAndMatching_JoinsEveryMemberToTheSameStructure() {
        val core = place(pair, BlockPos.ZERO, "core")
        val wing = place(pair, BlockPos(1, 0, 0), "wing")
        core.load()
        wing.load()

        assertNotNull(core.membership)
        assertEquals(core.membership!!.structureId, wing.membership!!.structureId)
        assertEquals(BlockPos.ZERO, core.membership!!.offset)
        assertEquals(BlockPos(1, 0, 0), wing.membership!!.offset)
        assertSame(core.part, manager.get(core.membership!!.structureId)!!.memberAt(BlockPos.ZERO))
        assertSame(wing.part, manager.get(core.membership!!.structureId)!!.memberAt(BlockPos(1, 0, 0)))
    }

    /**
     * [TestShapes.TRIPLE] needs a `wing` at both (1,0,0) and (-1,0,0). The west wing loads first, so its search must
     * reject offset (1,0,0) (wrong anchor, nothing there) before trying (-1,0,0).
     */
    @Test
    fun OnPartLoaded_RoleAtMultipleOffsets_TriesEachCandidateOffsetUntilOneMatches() {
        val triple = TestShapes.TRIPLE
        val core = place(triple, BlockPos.ZERO, "core")
        val east = place(triple, BlockPos(1, 0, 0), "wing")
        val west = place(triple, BlockPos(-1, 0, 0), "wing")
        west.load()

        val id = core.membership?.structureId
        assertNotNull(id)
        assertEquals(id, east.membership?.structureId)
        assertEquals(BlockPos(1, 0, 0), east.membership?.offset)
        assertEquals(BlockPos(-1, 0, 0), west.membership?.offset)
    }

    @Test
    fun OnPartLoaded_MissingNeighbor_DoesNotForm() {
        val core = place(pair, BlockPos.ZERO, "core")
        core.load()
        assertNull(core.membership)
    }

    @Test
    fun OnPartLoaded_WrongRoleAtSlot_DoesNotForm() {
        val wrong = place(pair, BlockPos.ZERO, "wing")
        val wing = place(pair, BlockPos(1, 0, 0), "wing")
        wrong.load()
        wing.load()
        assertNull(wrong.membership)
        assertNull(wing.membership)
    }

    /**
     * Forming never loads a chunk to look ([TestableLevel] fails the test on a lookup in an unloaded chunk).
     */
    @Test
    fun OnPartLoaded_SlotUnloaded_DoesNotFormOrLookItUp() {
        val core = place(pair, BlockPos.ZERO, "core")
        place(pair, BlockPos(1, 0, 0), "wing")
        level.unloaded += BlockPos(1, 0, 0)
        core.load()
        assertNull(core.membership)
    }

    @Test
    fun OnPartLoaded_AutoFormOff_DoesNotForm() {
        val core = place(pair, BlockPos.ZERO, "core", autoForm = false)
        val wing = place(pair, BlockPos(1, 0, 0), "wing", autoForm = false)
        core.load()
        wing.load()
        assertNull(core.membership)
    }

    @Test
    fun Form_AutoFormOff_FormsAtTheGivenAnchor() {
        val core = place(pair, BlockPos(5, 0, 0), "core", autoForm = false)
        val wing = place(pair, BlockPos(6, 0, 0), "wing", autoForm = false)
        val instance = manager.form(level, pair, BlockPos(5, 0, 0))
        assertNotNull(instance)
        assertEquals(BlockPos(5, 0, 0), instance!!.anchorPos)
        assertEquals(instance.id, core.membership?.structureId)
        assertEquals(instance.id, wing.membership?.structureId)
    }

    @Test
    fun Form_SlotAlreadyInAStructure_NoMemberJoins() {
        val core = place(pair, BlockPos.ZERO, "core")
        val wing = place(pair, BlockPos(1, 0, 0), "wing")
        wing.part.join(MultiblockMembership(UUID.randomUUID(), pair, BlockPos(1, 0, 0)))
        assertNull(manager.form(level, pair, BlockPos.ZERO))
        assertNull(core.membership)
    }

    @Test
    fun OnPartRemoved_Dissolve_OtherMembersLeave() {
        val core = place(pair, BlockPos.ZERO, "core")
        val wing = place(pair, BlockPos(1, 0, 0), "wing")
        core.load()
        val id = core.membership!!.structureId

        wing.remove()
        assertTrue(destroyed.isEmpty(), "DISSOLVE must not destroy any block")
        assertNull(core.membership, "the other member must leave when a sibling is removed")
        assertNull(manager.get(id), "the broken structure must be forgotten")
    }

    @Test
    fun OnPartRemoved_DestroyAll_DestroysEveryOtherMember() {
        val triple = TestShapes.LINKED_TRIPLE
        val core = place(triple, BlockPos.ZERO, "core")
        val east = place(triple, BlockPos(1, 0, 0), "wing")
        val west = place(triple, BlockPos(-1, 0, 0), "wing")
        core.load()
        val id = core.membership!!.structureId

        east.remove()

        assertEquals(setOf(BlockPos.ZERO, BlockPos(-1, 0, 0)), destroyed.toSet())
        assertNull(core.membership)
        assertNull(west.membership)
        assertNull(manager.get(id))
    }

    /**
     * The sibling's chunk unloaded, so the manager no longer holds it; it is still found through the level (which
     * loads chunks) and destroyed rather than left as an orphaned piece.
     */
    @Test
    fun OnPartRemoved_DestroyAll_SiblingUnloaded_StillDestroyed() {
        val triple = TestShapes.LINKED_TRIPLE
        val core = place(triple, BlockPos.ZERO, "core")
        val east = place(triple, BlockPos(1, 0, 0), "wing")
        val west = place(triple, BlockPos(-1, 0, 0), "wing")
        core.load()
        west.unload()

        east.remove()

        assertTrue(BlockPos(-1, 0, 0) in destroyed, "unloaded sibling must be destroyed")
        assertNull(west.membership)
    }

    @Test
    fun OnPartRemoved_DestroyAll_LeavesBlocksOfOtherStructures() {
        val triple = TestShapes.LINKED_TRIPLE
        val core = place(triple, BlockPos.ZERO, "core")
        val east = place(triple, BlockPos(1, 0, 0), "wing")
        place(triple, BlockPos(-1, 0, 0), "wing")
        core.load()
        val stranger = place(triple, BlockPos(-1, 0, 0), "wing")
        stranger.part.join(MultiblockMembership(UUID.randomUUID(), triple, BlockPos(-1, 0, 0)))

        east.remove()

        assertEquals(listOf(BlockPos.ZERO), destroyed)
        assertNotNull(stranger.membership)
    }

    /**
     * Destroying a sibling reports that sibling's own removal back to the manager; nothing is torn down twice.
     */
    @Test
    fun OnPartRemoved_DestroyAll_ReentrantRemovalIsIgnored() {
        val triple = TestShapes.LINKED_TRIPLE
        val parts = HashMap<BlockPos, Part>()
        val calls = ArrayList<BlockPos>()
        manager = MultiblockManager({ lvl, pos ->
            calls.add(pos)
            manager.onPartRemoved(lvl, pos, parts[pos]!!.part)
        }, tickets)
        listOf(BlockPos.ZERO to "core", BlockPos(1, 0, 0) to "wing", BlockPos(-1, 0, 0) to "wing").forEach { (pos, role) ->
            parts[pos] = place(triple, pos, role)
        }
        parts[BlockPos.ZERO]!!.load()
        val east = parts[BlockPos(1, 0, 0)]!!
        // A real block keeps its membership until it is gone; the reentrant call sees it.
        val westPart = parts[BlockPos(-1, 0, 0)]!!

        east.remove()

        assertEquals(setOf(BlockPos.ZERO, BlockPos(-1, 0, 0)), calls.toSet())
        assertEquals(2, calls.size)
        assertNull(westPart.membership)
    }

    @Test
    fun OnPartUnloaded_DoesNotBreakStructure_JustDeregistersLocally() {
        val core = place(pair, BlockPos.ZERO, "core")
        val wing = place(pair, BlockPos(1, 0, 0), "wing")
        core.load()
        val id = core.membership!!.structureId

        wing.unload()

        assertNotNull(core.membership)
        assertNotNull(wing.membership, "unloading must not clear the unloaded member's own membership")
        assertNull(manager.get(id)!!.memberAt(BlockPos(1, 0, 0)))
    }

    @Test
    fun OnPartUnloaded_LastMember_ForgetsTheStructure() {
        val core = place(pair, BlockPos.ZERO, "core")
        val wing = place(pair, BlockPos(1, 0, 0), "wing")
        core.load()
        val id = core.membership!!.structureId
        wing.unload()
        core.unload()
        assertNull(manager.get(id))
    }

    /**
     * A server restart: a fresh manager, and a member whose membership was restored from its save.
     */
    @Test
    fun OnPartLoaded_WithExistingMembership_RejoinsWithoutReformingOrNewId() {
        val core = place(pair, BlockPos.ZERO, "core")
        val wing = place(pair, BlockPos(1, 0, 0), "wing")
        core.load()
        val id = core.membership!!.structureId

        manager = MultiblockManager({ _, pos -> destroyed.add(pos) }, tickets)
        wing.load()

        assertEquals(id, wing.membership!!.structureId, "reload must not mint a new id")
        assertSame(wing.part, manager.get(id)!!.memberAt(BlockPos(1, 0, 0)))
        assertNotNull(wing.membership, "the home member still belongs to the structure")
    }

    /**
     * The structure broke while this member's chunk was unloaded (a DISSOLVE break it never saw): when it
     * loads again, the home member no longer belongs to the structure, so it leaves.
     */
    @Test
    fun OnPartLoaded_HomeLeftWhileUnloaded_MemberLeaves() {
        val core = place(pair, BlockPos.ZERO, "core")
        val wing = place(pair, BlockPos(1, 0, 0), "wing")
        core.load()
        wing.unload()
        manager.disband(level, core.pos, core.part)
        assertNotNull(wing.membership, "the unloaded wing was not told")

        wing.load()
        assertNull(wing.membership)
    }

    /**
     * Verification never loads the home chunk: it waits until the home position is loaded.
     */
    @Test
    fun VerifyPending_HomeChunkUnloaded_WaitsUntilLoaded() {
        val core = place(pair, BlockPos.ZERO, "core")
        val wing = place(pair, BlockPos(1, 0, 0), "wing")
        core.load()
        val id = core.membership!!.structureId
        manager = MultiblockManager({ _, pos -> destroyed.add(pos) }, tickets)
        level.removeIBlockEntity(BlockPos.ZERO)
        level.unloaded += BlockPos.ZERO

        wing.load()
        manager.verifyPending()
        assertNotNull(wing.membership, "left before the home member could be checked")

        level.unloaded -= BlockPos.ZERO
        manager.verifyPending()
        assertNull(wing.membership, "the home position holds no member of the structure")
        assertNull(manager.get(id))
    }

    @Test
    fun Disband_EveryMemberLeavesWithoutBreakEffects() {
        val shape = TestShapes.STATEFUL_LINKED_PAIR
        val core = place(shape, BlockPos.ZERO, "core")
        val wing = place(shape, BlockPos(1, 0, 0), "wing")
        core.load()
        val state = core.part.state as TestCounter

        manager.disband(level, wing.pos, wing.part)

        assertNull(core.membership)
        assertNull(wing.membership)
        assertNull(core.part.state, "the home member drops its state")
        assertTrue(destroyed.isEmpty())
        assertTrue(state.breaks.isEmpty())
    }
}

class MultiblockStateTest : MultiblockTestBase() {
    private val shape get() = TestShapes.STATEFUL_PAIR

    @Test
    fun Join_OnlyTheHomeMemberHoldsTheState() {
        val core = place(shape, BlockPos.ZERO, "core")
        val wing = place(shape, BlockPos(1, 0, 0), "wing")
        core.load()
        assertTrue(core.part.state is TestCounter)
        assertNull(wing.part.state)
        assertSame(core.part.state, wing.part.sharedState())
        assertSame(core.part.state, manager.stateOf(wing.part))
    }

    @Test
    fun SharedState_HomeUnloaded_Null() {
        val core = place(shape, BlockPos.ZERO, "core")
        val wing = place(shape, BlockPos(1, 0, 0), "wing")
        core.load()
        core.unload()
        assertNull(wing.part.sharedState())
    }

    @Test
    fun StateChange_MarksTheHomeMemberDirty() {
        val core = place(shape, BlockPos.ZERO, "core")
        place(shape, BlockPos(1, 0, 0), "wing")
        core.load()
        val before = core.be.dirtyCount
        (core.part.sharedState() as TestCounter).count++
        assertEquals(before + 1, core.be.dirtyCount)
    }

    @Test
    fun OnPartRemoved_StateToldOnceWithAnchorAndBrokenPosition() {
        val core = place(shape, BlockPos(4, 0, 0), "core")
        val wing = place(shape, BlockPos(5, 0, 0), "wing")
        core.load()
        val state = core.part.state as TestCounter

        wing.remove()

        assertEquals(listOf(BlockPos(4, 0, 0) to BlockPos(5, 0, 0)), state.breaks)
        assertNull(core.part.state)
    }

    /**
     * The home member is not registered (its chunk unloaded), so it is looked up through the level for the break.
     */
    @Test
    fun OnPartRemoved_HomeUnregistered_StateStillTold() {
        val core = place(shape, BlockPos.ZERO, "core")
        val wing = place(shape, BlockPos(1, 0, 0), "wing")
        core.load()
        val state = core.part.state as TestCounter
        core.unload()

        wing.remove()

        assertEquals(1, state.breaks.size)
        assertNull(core.membership, "the home member leaves too")
    }

    @Test
    fun Serialization_HomeSavesStateAndMembership_OtherMembersOnlyMembership() {
        val core = place(shape, BlockPos.ZERO, "core")
        val wing = place(shape, BlockPos(1, 0, 0), "wing")
        core.load()
        (core.part.state as TestCounter).count = 7

        val coreTag = TestIO.write { core.part.serializeTo(NBTSerializationScope.LEVEL, it) }
        val wingTag = TestIO.write { wing.part.serializeTo(NBTSerializationScope.LEVEL, it) }
        assertFalse(wingTag.contains(FragMultiblockPart.STATE_KEY))

        val loaded = FragMultiblockPart(listOf(MultiblockRoleRef(shape, "core"))) { manager }
        loaded.deserialize(TestIO.read(coreTag), NBTSerializationScope.LEVEL)
        assertEquals(core.membership, loaded.membership)
        assertEquals(7, (loaded.state as TestCounter).count)
    }

    @Test
    fun Serialization_Description_MembershipOnly() {
        val core = place(shape, BlockPos.ZERO, "core")
        place(shape, BlockPos(1, 0, 0), "wing")
        core.load()
        val tag = TestIO.write { core.part.serializeTo(NBTSerializationScope.DESCRIPTION, it) }
        assertFalse(tag.contains(FragMultiblockPart.STATE_KEY))
        val client = FragMultiblockPart(listOf(MultiblockRoleRef(shape, "core"))) { manager }
        client.deserialize(TestIO.read(tag), NBTSerializationScope.DESCRIPTION)
        assertEquals(core.membership, client.membership)
    }

    @Test
    fun Deserialize_UnknownShape_NotFormed() {
        val core = place(shape, BlockPos.ZERO, "core")
        place(shape, BlockPos(1, 0, 0), "wing")
        core.load()
        val tag = TestIO.write { core.part.serializeTo(NBTSerializationScope.LEVEL, it) }
        tag.putString(FragMultiblockPart.SHAPE_TAG, "itszulib_test:never_registered")
        val loaded = FragMultiblockPart(listOf()) { manager }
        loaded.deserialize(TestIO.read(tag), NBTSerializationScope.LEVEL)
        assertNull(loaded.membership)
        assertNull(loaded.state)
    }
}

class MultiblockTicketTest : MultiblockTestBase() {
    // The home member at x = 15 is in chunk 0; the wing at x = 16 is in chunk 1.
    private val home = BlockPos(15, 0, 0)
    private val other = BlockPos(16, 0, 0)

    @Test
    fun Register_MemberOutsideTheHomeChunk_RefreshesTheHomeChunkTicket() {
        place(TestShapes.STATEFUL_PAIR, home, "core").load()
        place(TestShapes.STATEFUL_PAIR, other, "wing").load()
        assertTrue(tickets.refreshed.isNotEmpty())
        assertTrue(tickets.refreshed.all { it == ChunkCoord.of(home) })
    }

    @Test
    fun Tick_RefreshesEveryInterval_WhileAMemberElsewhereTicks() {
        val core = place(TestShapes.STATEFUL_PAIR, home, "core")
        place(TestShapes.STATEFUL_PAIR, other, "wing")
        core.load()
        tickets.refreshed.clear()

        manager.tick(MultiblockManager.TICKET_REFRESH - 1)
        assertTrue(tickets.refreshed.isEmpty(), "refreshed between intervals")
        manager.tick(MultiblockManager.TICKET_REFRESH)
        assertEquals(listOf(ChunkCoord.of(home)), tickets.refreshed)
    }

    /**
     * A member in a chunk that does not tick (e.g. loaded only by another structure's ticket) never refreshes, so
     * structures cannot keep each other's chunks loaded.
     */
    @Test
    fun Tick_MemberElsewhereNotTicking_NoRefresh() {
        tickets.ticking = { false }
        place(TestShapes.STATEFUL_PAIR, home, "core").load()
        place(TestShapes.STATEFUL_PAIR, other, "wing")
        manager.tick(MultiblockManager.TICKET_REFRESH)
        assertTrue(tickets.refreshed.isEmpty())
    }

    @Test
    fun Tick_StatelessOrSameChunk_NoRefresh() {
        place(TestShapes.PAIR, home, "core").load()
        place(TestShapes.PAIR, other, "wing")
        place(TestShapes.STATEFUL_PAIR, BlockPos(0, 5, 0), "core").load()
        place(TestShapes.STATEFUL_PAIR, BlockPos(1, 5, 0), "wing")
        manager.tick(MultiblockManager.TICKET_REFRESH)
        assertTrue(tickets.refreshed.isEmpty())
    }

    @Test
    fun Tick_MemberElsewhereUnloaded_NoRefresh() {
        val core = place(TestShapes.STATEFUL_PAIR, home, "core")
        val wing = place(TestShapes.STATEFUL_PAIR, other, "wing")
        core.load()
        wing.unload()
        tickets.refreshed.clear()
        manager.tick(MultiblockManager.TICKET_REFRESH)
        assertTrue(tickets.refreshed.isEmpty())
    }
}

class FragMultiblockTickableTest : MultiblockTestBase() {
    @Test
    fun Tick_EveryMemberTicks_StructureTicksOncePerGameTick() {
        val core = place(TestShapes.STATEFUL_PAIR, BlockPos.ZERO, "core")
        val wing = place(TestShapes.STATEFUL_PAIR, BlockPos(1, 0, 0), "wing")
        var time = 100L
        val ticks = ArrayList<Long>()
        fun tickable(part: FragMultiblockPart) = object : FragMultiblockTickable(part, { time }) {
            override fun name(): String = "Tick"
            override fun serverStructureTick(level: ILevel, instance: MultiblockInstance) {
                ticks += time
            }
        }
        val a = tickable(core.part)
        val b = tickable(wing.part)
        a.tick(level, core.pos, Blocks.AIR.defaultBlockState())
        core.load()
        a.tick(level, core.pos, Blocks.AIR.defaultBlockState())
        b.tick(level, wing.pos, Blocks.AIR.defaultBlockState())
        time++
        b.tick(level, wing.pos, Blocks.AIR.defaultBlockState())
        a.tick(level, core.pos, Blocks.AIR.defaultBlockState())
        assertEquals(listOf(100L, 101L), ticks)
    }
}

class MultiblockShapeTest {
    @Test
    fun Register_DuplicateId_Throws() {
        val id = Identifier.fromNamespaceAndPath("itszulib_test", "dup")
        MultiblockShape.register(id, mapOf(BlockPos.ZERO to "a"))
        try {
            assertThrows(IllegalArgumentException::class.java) { MultiblockShape.register(id, mapOf(BlockPos.ZERO to "a")) }
        } finally {
            MultiblockShape.clear()
            TestShapes.clear()
        }
    }

    @Test
    fun Register_NoHomeSlot_Throws() {
        assertThrows(IllegalArgumentException::class.java) {
            MultiblockShape.register(Identifier.fromNamespaceAndPath("itszulib_test", "homeless"), mapOf(BlockPos(1, 0, 0) to "a"))
        }
    }

    @Test
    fun ById_UnknownId_ReturnsNull() {
        assertNull(MultiblockShape.byId(Identifier.fromNamespaceAndPath("itszulib_test", "never_registered")))
    }

    @Test
    fun Box_EverySlotFromTheLowestCorner() {
        val slots = MultiblockShape.box(2, 3, 2, "frame")
        assertEquals(12, slots.size)
        assertEquals("frame", slots[BlockPos.ZERO])
        assertEquals("frame", slots[BlockPos(1, 2, 1)])
        assertNull(slots[BlockPos(2, 0, 0)])
    }
}

class MultiblockSidedConfigurationTest : MultiblockTestBase() {
    private val main = ItemStorageArray(1)
    private val storages: Map<String, IItemStorage> = mapOf("main" to main, "empty" to IItemStorage.Empty)

    /**
     * A formed pair along x; the configuration belongs to the part at the origin, whose front is [front].
     */
    private fun config(front: Direction): Pair<MultiblockSidedItemStorageConfiguration, Part> {
        val core = place(TestShapes.PAIR, BlockPos.ZERO, "core")
        place(TestShapes.PAIR, BlockPos(1, 0, 0), "wing")
        core.load()
        val config = MultiblockSidedItemStorageConfiguration({ level }, { BlockPos.ZERO }, core.part, "empty", { "main" }, storages, { front })
        return config to core
    }

    /**
     * The IO stored for [relative], bypassing the multiblock overrides (read back from the saved form).
     */
    private fun storedIO(config: SidedStorageConfiguration<*>, relative: Direction): EnumAutomaticIO {
        val tag = TestIO.write(config::serialize)
        return EnumAutomaticIO.valueOf(tag.getStringOr("io${relative.ordinal}", "NONE"))
    }

    @Test
    fun InternalFace_ExposesEmptyStorageAndNoIO() {
        val (config, _) = config(Direction.NORTH)
        config.cycleRelativeFacingIOForward(Direction.WEST) // external, for contrast
        assertSame(IItemStorage.Empty, config.getStorageForGlobalFacing(Direction.EAST))
        assertSame(main, config.getStorageForGlobalFacing(Direction.WEST))
        assertEquals(EnumAutomaticIO.NONE, config.getIOForAbsoluteFacing(Direction.EAST))
        assertEquals(EnumAutomaticIO.INPUT, config.getIOForAbsoluteFacing(Direction.WEST))
    }

    /**
     * Regression: 1.12.2 passed the relative face to a check that expects a world face, so with a rotated front the
     * wrong faces were locked.
     */
    @Test
    fun CycleRelative_RotatedFront_LocksTheRelativeFaceThatIsInternal() {
        // Front EAST: the world EAST face (internal) is relative NORTH; relative EAST is world SOUTH (external).
        val (config, _) = config(Direction.EAST)
        config.cycleRelativeFacingIOForward(Direction.NORTH)
        config.cycleRelativeFacingStorageForward(Direction.NORTH)
        config.cycleRelativeFacingIOForward(Direction.EAST)
        assertEquals(EnumAutomaticIO.NONE, storedIO(config, Direction.NORTH))
        assertEquals(EnumAutomaticIO.INPUT, config.getIOForAbsoluteFacing(Direction.SOUTH))
        assertEquals(EnumAutomaticIO.NONE, config.getIOForAbsoluteFacing(Direction.EAST))
    }

    /**
     * Regression: relative queries (what a side configuration screen shows) agree with the world-face queries that
     * decide what the face does. 1.12.2 only overrode the world-face ones.
     */
    @Test
    fun InternalFace_RelativeQueriesAgreeWithAbsolute() {
        val (config, _) = config(Direction.EAST)
        assertEquals("empty", config.getStorageNameForRelativeFacing(Direction.NORTH))
        assertSame(IItemStorage.Empty, config.getStorageForRelativeFacing(Direction.NORTH))
        assertEquals(EnumAutomaticIO.NONE, config.getIOForRelativeFacing(Direction.NORTH))
        assertEquals("main", config.getStorageNameForRelativeFacing(Direction.EAST))
    }

    @Test
    fun Broken_EveryFaceConfigurable() {
        val (config, core) = config(Direction.NORTH)
        manager.disband(level, core.pos, core.part)
        config.cycleRelativeFacingIOForward(Direction.EAST)
        assertSame(main, config.getStorageForGlobalFacing(Direction.EAST))
        assertEquals(EnumAutomaticIO.INPUT, config.getIOForAbsoluteFacing(Direction.EAST))
    }

    /**
     * Regression (R5): deciding whether a face is internal never loads the neighbour's chunk.
     */
    @Test
    fun NeighbourUnloaded_TreatedAsExternalWithoutLookingItUp() {
        val (config, _) = config(Direction.NORTH)
        level.unloaded += BlockPos(1, 0, 0)
        assertSame(main, config.getStorageForGlobalFacing(Direction.EAST))
    }

    @Test
    fun NoLevel_TreatsEveryFaceAsExternal() {
        val (_, core) = config(Direction.NORTH)
        val config = MultiblockSidedItemStorageConfiguration({ null }, { BlockPos.ZERO }, core.part, "empty", { "main" }, storages, { Direction.NORTH })
        assertSame(main, config.getStorageForGlobalFacing(Direction.EAST))
    }

    @Test
    fun OtherStructure_FaceIsExternal() {
        val (config, _) = config(Direction.NORTH)
        val stranger = place(TestShapes.PAIR, BlockPos(0, 0, 1), "wing")
        stranger.part.join(MultiblockMembership(UUID.randomUUID(), TestShapes.PAIR, BlockPos(1, 0, 0)))
        assertSame(main, config.getStorageForGlobalFacing(Direction.SOUTH))
        config.cycleRelativeFacingIOForward(Direction.SOUTH)
        assertEquals(EnumAutomaticIO.INPUT, config.getIOForAbsoluteFacing(Direction.SOUTH))
    }

    @Test
    fun Fluid_InternalFace_ExposesEmptyStorageAndLocksCycling() {
        val (_, core) = config(Direction.NORTH)
        val tank = FluidStorageArray(1, 1000)
        val config = MultiblockSidedFluidStorageConfiguration(
            { level }, { BlockPos.ZERO }, core.part, "empty", { "tank" }, mapOf("tank" to tank, "empty" to IFluidStorage.Empty), { Direction.NORTH },
        )
        config.cycleRelativeFacingIOForward(Direction.EAST)
        assertSame(IFluidStorage.Empty, config.getStorageForGlobalFacing(Direction.EAST))
        assertSame(tank, config.getStorageForGlobalFacing(Direction.WEST))
        assertEquals(EnumAutomaticIO.NONE, config.getIOForRelativeFacing(Direction.EAST))
        assertEquals(EnumAutomaticIO.NONE, storedIO(config, Direction.EAST))
    }
}
