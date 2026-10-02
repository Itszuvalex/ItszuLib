package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.TestIO
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.storage.ItemStorageArray
import com.itszuvalex.itszulib.api.utility.DirectionUtil
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SidedStorageConfigurationTest {
    private class TestState {
        val keyA = "A"
        val keyB = "B"
        val keyC = "C"
        val map: Map<String, IItemStorage> = mapOf(keyA to ItemStorageArray(0), keyB to ItemStorageArray(0), keyC to ItemStorageArray(0))
        val defaults: (Direction) -> String = { dir -> map.keys.sorted()[dir.ordinal % map.size] }
        val front: () -> Direction = { Direction.NORTH }
        val config = SidedItemStorageConfiguration(defaults, map, front)
    }

    @Test
    fun CycleRelativeFacingStorageBackward_DecrementKeyAndWrapAroundZero() {
        val state = TestState()
        val facing = Direction.entries[1]
        Assertions.assertEquals(state.keyB, state.config.getStorageNameForRelativeFacing(facing))
        state.config.cycleRelativeFacingStorageBackward(facing)
        Assertions.assertEquals(state.keyA, state.config.getStorageNameForRelativeFacing(facing))
        state.config.cycleRelativeFacingStorageBackward(facing)
        Assertions.assertEquals(state.keyC, state.config.getStorageNameForRelativeFacing(facing))
        state.config.cycleRelativeFacingStorageBackward(facing)
        Assertions.assertEquals(state.keyB, state.config.getStorageNameForRelativeFacing(facing))
    }

    @Test
    fun CycleRelativeFacingStorageForward_IncrementKeyAndWrapAroundEnd() {
        val state = TestState()
        val facing = Direction.entries[1]
        state.config.cycleRelativeFacingStorageForward(facing)
        Assertions.assertEquals(state.keyC, state.config.getStorageNameForRelativeFacing(facing))
        state.config.cycleRelativeFacingStorageForward(facing)
        Assertions.assertEquals(state.keyA, state.config.getStorageNameForRelativeFacing(facing))
        state.config.cycleRelativeFacingStorageForward(facing)
        Assertions.assertEquals(state.keyB, state.config.getStorageNameForRelativeFacing(facing))
    }

    @Test
    fun CycleRelativeFacingIOBackward_DecrementKeyAndWrapAroundZero() {
        val config = TestState().config
        val facing = Direction.entries[1]
        Assertions.assertEquals(EnumAutomaticIO.NONE, config.getIOForRelativeFacing(facing))
        config.cycleRelativeFacingIOBackward(facing)
        Assertions.assertEquals(EnumAutomaticIO.OUTPUT, config.getIOForRelativeFacing(facing))
        config.cycleRelativeFacingIOBackward(facing)
        Assertions.assertEquals(EnumAutomaticIO.INPUT, config.getIOForRelativeFacing(facing))
        config.cycleRelativeFacingIOBackward(facing)
        Assertions.assertEquals(EnumAutomaticIO.NONE, config.getIOForRelativeFacing(facing))
    }

    @Test
    fun CycleRelativeFacingIOForward_IncrementKeyAndWrapAroundEnd() {
        val config = TestState().config
        val facing = Direction.entries[1]
        config.cycleRelativeFacingIOForward(facing)
        Assertions.assertEquals(EnumAutomaticIO.INPUT, config.getIOForRelativeFacing(facing))
        config.cycleRelativeFacingIOForward(facing)
        Assertions.assertEquals(EnumAutomaticIO.OUTPUT, config.getIOForRelativeFacing(facing))
        config.cycleRelativeFacingIOForward(facing)
        Assertions.assertEquals(EnumAutomaticIO.NONE, config.getIOForRelativeFacing(facing))
    }

    @Test
    fun GetForAbsoluteFacing_RotatedFront_MapsWorldFaceToRelativeFace() {
        val state = TestState()
        val config = SidedItemStorageConfiguration(state.defaults, state.map) { Direction.EAST }
        config.cycleRelativeFacingIOForward(Direction.NORTH)
        for (dir in Direction.entries) {
            val relative = DirectionUtil.getHorizontalRelativeDirectionFromAbsolute(dir, Direction.EAST)
            Assertions.assertEquals(config.getStorageNameForRelativeFacing(relative), config.getStorageNameForAbsoluteFacing(dir))
        }
        Assertions.assertEquals(config.getStorageNameForRelativeFacing(Direction.NORTH), config.getStorageNameForAbsoluteFacing(Direction.EAST))
        Assertions.assertEquals(config.getStorageNameForRelativeFacing(Direction.UP), config.getStorageNameForAbsoluteFacing(Direction.UP))
        Assertions.assertEquals(EnumAutomaticIO.INPUT, config.getIOForAbsoluteFacing(Direction.EAST))
        Assertions.assertEquals(EnumAutomaticIO.NONE, config.getIOForAbsoluteFacing(Direction.NORTH))
    }

    @Test
    fun SerializeDeserialize_RoundTrip() {
        val state = TestState()
        state.config.cycleRelativeFacingStorageForward(Direction.UP)
        state.config.cycleRelativeFacingIOBackward(Direction.WEST)
        val loaded = TestState().config
        loaded.deserialize(TestIO.read(TestIO.write(state.config::serialize)))
        for (dir in Direction.entries) {
            Assertions.assertEquals(state.config.getStorageNameForRelativeFacing(dir), loaded.getStorageNameForRelativeFacing(dir))
            Assertions.assertEquals(state.config.getIOForRelativeFacing(dir), loaded.getIOForRelativeFacing(dir))
        }
    }

    @Test
    fun Deserialize_MissingOrUnknownData_FallBackWithoutThrowing() {
        val state = TestState()
        val tag = CompoundTag()
        tag.putString("0", "not a storage")
        tag.putInt("io0", 99)
        tag.putInt("io1", EnumAutomaticIO.INPUT.ordinal) // Older saves stored ordinals.
        val down = Direction.entries[0]
        val up = Direction.entries[1]
        val expectedDown = state.config.getStorageNameForRelativeFacing(down)
        val expectedUp = state.config.getStorageNameForRelativeFacing(up)

        state.config.deserialize(TestIO.read(tag))

        Assertions.assertEquals(expectedDown, state.config.getStorageNameForRelativeFacing(down))
        Assertions.assertEquals(expectedUp, state.config.getStorageNameForRelativeFacing(up))
        Assertions.assertEquals(EnumAutomaticIO.NONE, state.config.getIOForRelativeFacing(down))
        Assertions.assertEquals(EnumAutomaticIO.INPUT, state.config.getIOForRelativeFacing(up))
        Assertions.assertDoesNotThrow { state.config.cycleRelativeFacingStorageForward(down) }
    }

    @Test
    fun CycleRelativeFacingStorage_CurrentKeyUnknown_StartFromEnds() {
        val state = TestState()
        val config = SidedItemStorageConfiguration({ "missing" }, state.map, state.front)
        config.cycleRelativeFacingStorageForward(Direction.NORTH)
        Assertions.assertEquals(state.keyA, config.getStorageNameForRelativeFacing(Direction.NORTH))
        config.cycleRelativeFacingStorageBackward(Direction.SOUTH)
        Assertions.assertEquals(state.keyC, config.getStorageNameForRelativeFacing(Direction.SOUTH))
    }
}
