package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.TestIO
import com.itszuvalex.itszulib.TestableCoreBlockEntity
import com.itszuvalex.itszulib.TestableLevel
import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.storage.FluidStorageArray
import com.itszuvalex.itszulib.api.storage.IFluidStorage
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.storage.ItemStorageArray
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.core.EnumAutomaticIO
import com.itszuvalex.itszulib.core.SidedStorageConfiguration
import com.itszuvalex.itszulib.core.frag.FragMultiBlockInfo
import com.itszuvalex.itszulib.core.frag.FragMultiblockState
import com.itszuvalex.itszulib.core.frag.FragMultiblockTickable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.neoforge.common.util.ValueIOSerializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val ORIGIN: BlockPos = BlockPos.ZERO
private val EAST_OF_ORIGIN: BlockPos = BlockPos(1, 0, 0)

/**
 * A pattern that only lists positions; block matching needs a real level and is covered by the game tests.
 */
private class OffsetsPattern(private val offsets: List<BlockPos>) : IBlockPattern {
    override val rotation: Rotation = Rotation.NONE
    override fun rotated(rot: Rotation): IBlockPattern = OffsetsPattern(offsets.map { it.rotate(rot) })
    override fun matches(level: Level, pos: BlockPos): Boolean = true
    override fun blocksInMatch(pos: BlockPos): Collection<BlockPos> = offsets.map { pos.offset(it) }
}

private val TWO_WIDE = OffsetsPattern(listOf(BlockPos.ZERO, BlockPos(1, 0, 0)))

/**
 * A block entity with a [FragMultiBlockInfo] whose controller lookups go through [level].
 */
private fun part(level: TestableLevel, pos: BlockPos): Pair<TestableCoreBlockEntity, FragMultiBlockInfo> {
    val be = TestableCoreBlockEntity(pos, level)
    val frag = FragMultiBlockInfo()
    frag.levelOf = { level }
    be.fragList.addFragment(frag)
    return be to frag
}

class MultiBlockInfoTest {
    @Test
    fun Form_Fresh_JoinsAndOnlyTheControllerIsController() {
        val controller = MultiBlockInfo()
        val other = MultiBlockInfo()
        assertTrue(controller.form(ORIGIN, ORIGIN))
        assertTrue(other.form(EAST_OF_ORIGIN, ORIGIN))
        assertTrue(controller.isFormed && controller.isController)
        assertTrue(other.isFormed && !other.isController)
        assertEquals(ORIGIN, other.controller)
    }

    /**
     * Regression: 1.12.2 compared the block's own position (not the new controller's) with the stored controller, so a
     * formed controller could be taken over by another multiblock, and a formed part could not re-join its own.
     */
    @Test
    fun Form_AlreadyFormed_OnlySameControllerAccepted() {
        val controller = MultiBlockInfo()
        controller.form(ORIGIN, ORIGIN)
        assertFalse(controller.form(ORIGIN, EAST_OF_ORIGIN), "a formed controller joined another multiblock")
        assertEquals(ORIGIN, controller.controller)
        assertTrue(controller.isController)

        val other = MultiBlockInfo()
        other.form(EAST_OF_ORIGIN, ORIGIN)
        assertTrue(other.form(EAST_OF_ORIGIN, ORIGIN), "a formed part could not re-join its own multiblock")
    }

    @Test
    fun BreakFrom_OnlyOwnControllerAccepted() {
        val info = MultiBlockInfo()
        info.form(EAST_OF_ORIGIN, ORIGIN)
        assertFalse(info.breakFrom(EAST_OF_ORIGIN))
        assertTrue(info.isFormed)
        assertTrue(info.breakFrom(ORIGIN))
        assertFalse(info.isFormed)
        assertNull(info.controller)
    }

    @Test
    fun OnChanged_RunsOnFormAndBreakOnly() {
        val info = MultiBlockInfo()
        var changes = 0
        info.onChanged = Runnable { changes++ }
        info.form(ORIGIN, ORIGIN)
        info.form(ORIGIN, EAST_OF_ORIGIN) // refused
        info.breakFrom(ORIGIN)
        assertEquals(2, changes)
    }

    @Test
    fun Serialization_RoundTrips() {
        val info = MultiBlockInfo()
        info.form(EAST_OF_ORIGIN, BlockPos(5, 6, 7))
        val copy = MultiBlockInfo()
        copy.deserialize(TestIO.read(TestIO.write(info::serialize)))
        assertTrue(copy.isFormed)
        assertFalse(copy.isController)
        assertEquals(BlockPos(5, 6, 7), copy.controller)
    }

    @Test
    fun Deserialize_Missing_IsNotFormed() {
        val info = MultiBlockInfo()
        info.form(ORIGIN, ORIGIN)
        info.deserialize(TestIO.read(TestIO.write { }))
        assertFalse(info.isFormed)
        assertFalse(info.isController)
    }
}

class BlockPatternStaticTest {
    private fun offsets(pattern: IBlockPattern) = pattern.blocksInMatch(BlockPos.ZERO).toSet()

    private fun twoWide() = BlockPatternStatic(mapOf(BlockPos.ZERO to Blocks.STONE, BlockPos(1, 0, 0) to Blocks.STONE))

    /**
     * Regression: 1.12.2's `rotated` rotated a throwaway copy and returned an unrotated one.
     */
    @Test
    fun Rotated_RotatesOffsetsAndRecordsRotation() {
        val rotated = twoWide().rotated(Rotation.CLOCKWISE_90)
        assertEquals(setOf(BlockPos.ZERO, BlockPos(0, 0, 1)), offsets(rotated))
        assertEquals(Rotation.CLOCKWISE_90, rotated.rotation)
        assertEquals(Rotation.CLOCKWISE_180, rotated.rotated(Rotation.CLOCKWISE_90).rotation)
    }

    @Test
    fun Rotated_LeavesOriginalUnchanged() {
        val pattern = twoWide()
        pattern.rotated(Rotation.CLOCKWISE_180)
        assertEquals(setOf(BlockPos.ZERO, BlockPos(1, 0, 0)), offsets(pattern))
        assertEquals(Rotation.NONE, pattern.rotation)
    }

    @Test
    fun Constructor_CopiesTheMap() {
        val map = hashMapOf(BlockPos.ZERO to Blocks.STONE)
        val pattern = BlockPatternStatic(map)
        map[BlockPos(1, 0, 0)] = Blocks.STONE
        assertEquals(1, pattern.blocks.size)
    }

    @Test
    fun BlocksInMatch_OffsetsFromController() {
        val pattern = BlockPatternStatic(mapOf(BlockPos.ZERO to Blocks.STONE, BlockPos(0, 1, 0) to Blocks.STONE))
        assertEquals(setOf(BlockPos(10, 20, 30), BlockPos(10, 21, 30)), pattern.blocksInMatch(BlockPos(10, 20, 30)).toSet())
    }

    @Test
    fun OverChunkBoundaries_DetectsSpanningPatterns() {
        val pattern = twoWide()
        assertFalse(pattern.overChunkBoundaries(BlockPos(0, 64, 0)))
        assertTrue(pattern.overChunkBoundaries(BlockPos(15, 64, 0)))
        assertTrue(pattern.overChunkBoundaries(BlockPos(-1, 64, 0)))
    }
}

class MultiblockStaticTest {
    @Test
    fun Form_EveryPartJoinsTheController() {
        val level = TestableLevel()
        val (_, a) = part(level, ORIGIN)
        val (_, b) = part(level, EAST_OF_ORIGIN)
        assertTrue(MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE))
        assertTrue(a.info.isController)
        assertEquals(ORIGIN, b.info.controller)
        assertFalse(b.info.isController)
    }

    // 1.12.2 kept forming after a part refused, leaving a half-formed multiblock whose controller ran while a part
    // belonged to nothing, or to another multiblock.
    @Test
    fun Form_PartWithoutModule_ReturnsFalseAndNoPartJoins() {
        val level = TestableLevel()
        val (_, a) = part(level, ORIGIN)
        TestableCoreBlockEntity(EAST_OF_ORIGIN, level)
        assertFalse(MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE))
        assertFalse(a.info.isFormed)
    }

    @Test
    fun Form_PartMissing_ReturnsFalseAndNoPartJoins() {
        val level = TestableLevel()
        val (_, a) = part(level, ORIGIN)
        assertFalse(MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE))
        assertFalse(a.info.isFormed)
    }

    @Test
    fun Form_PartOfAnotherMultiblock_ReturnsFalseAndNoPartJoins() {
        val level = TestableLevel()
        val (_, a) = part(level, ORIGIN)
        val (_, b) = part(level, EAST_OF_ORIGIN)
        b.info.form(EAST_OF_ORIGIN, BlockPos(9, 9, 9))
        assertFalse(MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE))
        assertFalse(a.info.isFormed)
        assertEquals(BlockPos(9, 9, 9), b.info.controller)
    }

    @Test
    fun Form_AlreadyFormedBySameController_Succeeds() {
        val level = TestableLevel()
        part(level, ORIGIN)
        part(level, EAST_OF_ORIGIN)
        val mb = MultiblockStatic(TWO_WIDE)
        assertTrue(mb.form(level, ORIGIN, TWO_WIDE))
        assertTrue(mb.form(level, ORIGIN, TWO_WIDE))
    }

    @Test
    fun BreakMultiblock_EveryPartLeaves() {
        val level = TestableLevel()
        val (_, a) = part(level, ORIGIN)
        val (_, b) = part(level, EAST_OF_ORIGIN)
        val mb = MultiblockStatic(TWO_WIDE)
        mb.form(level, ORIGIN, TWO_WIDE)
        assertTrue(mb.breakMultiblock(level, ORIGIN, TWO_WIDE))
        assertFalse(a.info.isFormed)
        assertFalse(b.info.isFormed)
    }

    @Test
    fun BreakMultiblock_MissingPart_ReturnsFalseButOthersLeave() {
        val level = TestableLevel()
        val (_, a) = part(level, ORIGIN)
        a.info.form(ORIGIN, ORIGIN)
        assertFalse(MultiblockStatic(TWO_WIDE).breakMultiblock(level, ORIGIN, TWO_WIDE))
        assertFalse(a.info.isFormed)
    }
}

class MultiblockUtilsTest {
    @Test
    fun IsFacingInMultiblock_SameController_True() {
        val level = TestableLevel()
        val (_, a) = part(level, ORIGIN)
        part(level, EAST_OF_ORIGIN)
        MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE)
        assertTrue(MultiblockUtils.isFacingInMultiblock(level, ORIGIN, Direction.EAST, a.info))
        assertFalse(MultiblockUtils.isFacingInMultiblock(level, ORIGIN, Direction.WEST, a.info))
    }

    @Test
    fun IsFacingInMultiblock_OtherControllerOrNotFormed_False() {
        val level = TestableLevel()
        val (_, a) = part(level, ORIGIN)
        val (_, b) = part(level, EAST_OF_ORIGIN)
        assertFalse(MultiblockUtils.isFacingInMultiblock(level, ORIGIN, Direction.EAST, a.info))
        a.info.form(ORIGIN, ORIGIN)
        b.info.form(EAST_OF_ORIGIN, EAST_OF_ORIGIN)
        assertFalse(MultiblockUtils.isFacingInMultiblock(level, ORIGIN, Direction.EAST, a.info))
    }

    /**
     * Regression: the lookup must not load the neighbour's chunk (1.12.2 asked with force = false; a plain
     * `Level#getBlockEntity` on the server loads the chunk).
     */
    @Test
    fun IsFacingInMultiblock_NeighbourUnloaded_FalseWithoutLookingItUp() {
        val level = TestableLevel()
        val (_, a) = part(level, ORIGIN)
        part(level, EAST_OF_ORIGIN)
        MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE)
        level.unloaded += EAST_OF_ORIGIN
        assertFalse(MultiblockUtils.isFacingInMultiblock(level, ORIGIN, Direction.EAST, a.info))
    }

    @Test
    fun Controller_FormedAndLoaded_ReturnsIt() {
        val level = TestableLevel()
        val (controller, _) = part(level, ORIGIN)
        val (_, b) = part(level, EAST_OF_ORIGIN)
        assertNull(MultiblockUtils.controller(level, b.info))
        MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE)
        assertSame(controller, MultiblockUtils.controller(level, b.info))
        level.unloaded += ORIGIN
        assertNull(MultiblockUtils.controller(level, b.info))
    }
}

class MultiblockSidedConfigurationTest {
    private val main = ItemStorageArray(1)
    private val storages: Map<String, IItemStorage> = mapOf("main" to main, "empty" to IItemStorage.Empty)

    /**
     * Two formed parts side by side along x; the configuration belongs to the one at the origin, whose front is
     * [front].
     */
    private fun config(front: Direction): Pair<MultiblockSidedItemStorageConfiguration, TestableLevel> {
        val level = TestableLevel()
        val (_, a) = part(level, ORIGIN)
        part(level, EAST_OF_ORIGIN)
        MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE)
        val config = MultiblockSidedItemStorageConfiguration(
            { level }, { ORIGIN }, a.info, "empty", { "main" }, storages, { front },
        )
        return config to level
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
     * Regression: relative queries (what a side configuration screen shows) must agree with the world-face queries
     * that decide what the face does. 1.12.2 only overrode the world-face ones.
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
        val (config, level) = config(Direction.NORTH)
        MultiblockStatic(TWO_WIDE).breakMultiblock(level, ORIGIN, TWO_WIDE)
        config.cycleRelativeFacingIOForward(Direction.EAST)
        assertSame(main, config.getStorageForGlobalFacing(Direction.EAST))
        assertEquals(EnumAutomaticIO.INPUT, config.getIOForAbsoluteFacing(Direction.EAST))
    }

    @Test
    fun NoLevel_TreatsEveryFaceAsExternal() {
        val info = MultiBlockInfo().also { it.form(ORIGIN, ORIGIN) }
        val config = MultiblockSidedItemStorageConfiguration({ null }, { ORIGIN }, info, "empty", { "main" }, storages, { Direction.NORTH })
        assertSame(main, config.getStorageForGlobalFacing(Direction.EAST))
    }

    @Test
    fun Fluid_InternalFace_ExposesEmptyStorageAndLocksCycling() {
        val level = TestableLevel()
        val (_, a) = part(level, ORIGIN)
        part(level, EAST_OF_ORIGIN)
        MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE)
        val tank = FluidStorageArray(1, 1000)
        val config = MultiblockSidedFluidStorageConfiguration(
            { level }, { ORIGIN }, a.info, "empty", { "tank" }, mapOf("tank" to tank, "empty" to IFluidStorage.Empty), { Direction.NORTH },
        )
        config.cycleRelativeFacingIOForward(Direction.EAST)
        assertSame(IFluidStorage.Empty, config.getStorageForGlobalFacing(Direction.EAST))
        assertSame(tank, config.getStorageForGlobalFacing(Direction.WEST))
        assertEquals(EnumAutomaticIO.NONE, config.getIOForRelativeFacing(Direction.EAST))
        assertEquals(EnumAutomaticIO.NONE, storedIO(config, Direction.EAST))
    }
}

class Counter : ValueIOSerializable {
    var count = 0
    override fun serialize(output: ValueOutput) = output.putInt("count", count)
    override fun deserialize(input: ValueInput) {
        count = input.getIntOr("count", 0)
    }
}

class FragMultiblockStateTest {
    private val states = HashMap<com.itszuvalex.itszulib.api.adapters.IBlockEntity, FragMultiblockState<Counter>>()

    private inner class Part(level: TestableLevel, pos: BlockPos) {
        val be: TestableCoreBlockEntity
        val info: FragMultiBlockInfo
        val state: FragMultiblockState<Counter>

        init {
            val (b, i) = part(level, pos)
            be = b
            info = i
            state = FragMultiblockState(info, ::Counter, { other -> states[other] })
            be.fragList.addInternalFragment(state)
            states[be] = state
        }
    }

    private fun formed(): Triple<TestableLevel, Part, Part> {
        val level = TestableLevel()
        val a = Part(level, ORIGIN)
        val b = Part(level, EAST_OF_ORIGIN)
        MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE)
        return Triple(level, a, b)
    }

    @Test
    fun Get_NotFormed_Null() {
        val a = Part(TestableLevel(), ORIGIN)
        assertNull(a.state.get())
        assertFalse(a.state.doIfController { })
    }

    @Test
    fun Get_OnAnyPart_ReturnsTheControllersState() {
        val (_, a, b) = formed()
        b.state.get()!!.count = 3
        assertSame(a.state.get(), b.state.get())
        assertTrue(a.state.hasState())
        assertFalse(b.state.hasState())
        assertEquals(3, a.state.get()!!.count)
    }

    @Test
    fun DoIfController_RunsOnlyOnController() {
        val (_, a, b) = formed()
        assertTrue(a.state.doIfController { it.count++ })
        assertFalse(b.state.doIfController { it.count++ })
        assertEquals(1, a.state.get()!!.count)
    }

    @Test
    fun Serialization_OnlyControllerSavesAndLoads() {
        val (_, a, b) = formed()
        a.state.get()!!.count = 7
        val controllerTag = TestIO.write { a.be.fragList.serializeTo(NBTSerializationScope.LEVEL, it) }
        val partTag = TestIO.write { b.be.fragList.serializeTo(NBTSerializationScope.LEVEL, it) }
        assertTrue(controllerTag.getCompoundOrEmpty(FragMultiblockState.NAME).contains(FragMultiblockState.STATE_KEY))
        assertFalse(partTag.getCompoundOrEmpty(FragMultiblockState.NAME).contains(FragMultiblockState.STATE_KEY))

        // The info fragment was added first, so it loads first and the state knows it belongs to the controller.
        val reloaded = Part(TestableLevel(), ORIGIN)
        reloaded.be.fragList.deserialize(TestIO.read(controllerTag), NBTSerializationScope.LEVEL)
        assertEquals(7, reloaded.state.get()!!.count)
    }

    /**
     * Regression: a controller kept its state in memory after its multiblock broke, so a re-formed multiblock got the
     * old state back, but only if the chunk had not been reloaded in between (only a controller saves its state).
     */
    @Test
    fun BreakThenReform_StartsFromFreshState() {
        val (level, a, _) = formed()
        a.state.get()!!.count = 5
        MultiblockStatic(TWO_WIDE).breakMultiblock(level, ORIGIN, TWO_WIDE)
        assertNull(a.state.get())
        MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE)
        assertEquals(0, a.state.get()!!.count)
    }

    @Test
    fun Clear_DropsStateAndMarksDirty() {
        val (_, a, _) = formed()
        a.state.get()!!.count = 5
        val before = a.be.dirtyCount
        a.state.clear()
        assertFalse(a.state.hasState())
        assertTrue(a.be.dirtyCount > before)
    }
}

class FragMultiBlockInfoTest {
    @Test
    fun FormAndBreak_MarkDirtyAndSync() {
        val (be, frag) = part(TestableLevel(), ORIGIN)
        frag.info.form(ORIGIN, ORIGIN)
        frag.info.breakFrom(ORIGIN)
        assertEquals(2, be.syncCount)
    }

    @Test
    fun Scopes_LevelAndDescriptionNotItem() {
        val frag = FragMultiBlockInfo()
        assertTrue(frag.handlesScope(NBTSerializationScope.LEVEL))
        assertTrue(frag.handlesScope(NBTSerializationScope.DESCRIPTION))
        assertFalse(frag.handlesScope(NBTSerializationScope.ITEM))
    }

    @Test
    fun Module_ExposedOnEverySide() {
        val (be, frag) = part(TestableLevel(), ORIGIN)
        assertSame(frag.info, be.getModule(Modules.MULTIBLOCK, null))
        assertSame(frag.info, be.getModule(Modules.MULTIBLOCK, Direction.UP))
    }

    @Test
    fun ControllerModule_ResolvesThroughTheController() {
        val level = TestableLevel()
        val (controller, a) = part(level, ORIGIN)
        val (_, b) = part(level, EAST_OF_ORIGIN)
        MultiblockStatic(TWO_WIDE).form(level, ORIGIN, TWO_WIDE)
        assertSame(controller, b.controller())
        assertSame(a.info, b.controllerModule(Modules.MULTIBLOCK))
        assertNotSame(b.info, b.controllerModule(Modules.MULTIBLOCK))
    }
}

class FragMultiblockTickableTest {
    private class Recorder(info: MultiBlockInfo) : FragMultiblockTickable(info) {
        var server = 0
        var client = 0
        override fun name(): String = "Recorder"
        override fun serverControllerTick(level: ILevel, pos: BlockPos) {
            server++
        }
        override fun clientControllerTick(level: ILevel, pos: BlockPos) {
            client++
        }
    }

    private fun state(): BlockState = Blocks.STONE.defaultBlockState()

    @Test
    fun Tick_OnlyOnFormedController_RoutedBySide() {
        val level = TestableLevel()
        val info = MultiBlockInfo()
        val recorder = Recorder(info)
        recorder.tick(level, ORIGIN, state())
        info.form(EAST_OF_ORIGIN, ORIGIN)
        recorder.tick(level, ORIGIN, state())
        assertEquals(0, recorder.server)
        info.breakFrom(ORIGIN)
        info.form(ORIGIN, ORIGIN)
        recorder.tick(level, ORIGIN, state())
        level.clientSide = true
        recorder.tick(level, ORIGIN, state())
        assertEquals(1, recorder.server)
        assertEquals(1, recorder.client)
    }
}
