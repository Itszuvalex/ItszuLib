package com.itszuvalex.itszulib.util

import com.itszuvalex.itszulib.TestIO
import com.itszuvalex.itszulib.api.storage.PowerBattery
import com.itszuvalex.itszulib.client.ScreenMath
import com.itszuvalex.itszulib.core.HorizontalFacing
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FaceBitSetTest {
    @Test
    fun SetClearToggle_PerFace() {
        val set = FaceBitSet()
        set.set(Direction.UP)
        set[Direction.EAST] = true
        set.toggle(Direction.NORTH)
        assertEquals(listOf(Direction.UP, Direction.NORTH, Direction.EAST), set.faces())
        set.clear(Direction.UP)
        set.toggle(Direction.NORTH)
        assertEquals(listOf(Direction.EAST), set.faces())
        assertFalse(set[Direction.DOWN])
    }

    @Test
    fun NullFace_HasItsOwnBit() {
        val set = FaceBitSet()
        set.set(null)
        assertTrue(set[null])
        assertTrue(set.faces().isEmpty())
        Direction.entries.forEach { assertFalse(set[it]) }
        set.clear(null)
        assertTrue(set.isEmpty())
    }

    @Test
    fun AllFacesAndNull_RoundTripThroughBits() {
        val set = FaceBitSet()
        Direction.entries.forEach(set::set)
        set.set(null)
        val copy = FaceBitSet(set.bits)
        assertEquals(set, copy)
        assertTrue(copy[null])
        assertEquals(6, copy.faces().size)
    }

    @Test
    fun Load_DropsUnknownBits() {
        val set = FaceBitSet()
        set.load(-1)
        assertEquals(0x7F, set.bits)
    }
}

class TaskTest {
    @Test
    fun Contribute_SpreadsTheGoalOverMinTicks() {
        val task = Task(100.0, 4)
        assertEquals(25.0, task.powerPerTick(0.0, 0.0))
        repeat(3) { assertEquals(25.0, task.contribute(1000.0, 0.0, 0.0)) }
        assertFalse(task.completed(0.0))
        task.contribute(1000.0, 0.0, 0.0)
        assertTrue(task.completed(0.0))
        assertEquals(0.0, task.contribute(1000.0, 0.0, 0.0), "a completed task takes nothing")
    }

    @Test
    fun Contribute_LimitedByPowerOffered() {
        val task = Task(100.0, 4)
        assertEquals(10.0, task.contribute(10.0, 0.0, 0.0))
        assertEquals(10.0, task.progress)
    }

    @Test
    fun SpeedAndEfficiency_ScaleTicksAndGoal() {
        val task = Task(100.0, 10)
        assertEquals(5, task.adjustedTicks(1.0))
        assertEquals(20, task.adjustedTicks(-1.0))
        assertEquals(4, task.adjustedTicks(1.5), "rounded up")
        assertEquals(50.0, task.adjustedMax(1.0))
        assertEquals(200.0, task.adjustedMax(-1.0))
    }

    /**
     * Regression: a free, instant task (goal 0, 0 ticks) divided 0 by 0 in 1.12.2; progress became NaN and the task
     * never completed.
     */
    @Test
    fun FreeInstantTask_CompletesAfterAContribution() {
        val task = Task(0.0, 0)
        task.contribute(10.0, 0.0, 0.0)
        assertFalse(task.progress.isNaN())
        assertTrue(task.completed(0.0))
    }

    /**
     * Regression: negative power undid progress in 1.12.2.
     */
    @Test
    fun Contribute_NegativePower_DoesNothing() {
        val task = Task(100.0, 4)
        task.contribute(20.0, 0.0, 0.0)
        assertEquals(0.0, task.contribute(-50.0, 0.0, 0.0))
        assertEquals(20.0, task.progress)
    }

    @Test
    fun ContributeFrom_DrainsTheBattery() {
        val battery = PowerBattery(100.0).also { it.setStorage(30.0) }
        val task = Task(100.0, 2)
        assertEquals(30.0, task.contributeFrom(battery, 0.0, 0.0))
        assertEquals(0.0, battery.storage())
        assertEquals(30.0, task.progress)
    }

    @Test
    fun Fraction_ClampedProgress() {
        val task = Task(100.0, 4)
        task.contribute(25.0, 0.0, 0.0)
        assertEquals(0.25, task.fraction(0.0))
        assertEquals(1.0, Task(0.0, 0).fraction(0.0))
    }

    @Test
    fun Serialization_RoundTrips() {
        val task = Task(80.0, 6)
        task.contribute(5.0, 0.0, 0.0)
        val copy = Task()
        copy.deserialize(TestIO.read(TestIO.write(task::serialize)))
        assertEquals(5.0, copy.progress)
        assertEquals(80.0, copy.baseGoal)
        assertEquals(6, copy.minTicks)
    }
}

class ScreenMathTest {
    @Test
    fun FilledPixels_RoundsUpAndClamps() {
        assertEquals(0, ScreenMath.filledPixels(0, 1000, 50))
        assertEquals(1, ScreenMath.filledPixels(1, 1000, 50), "any fluid shows")
        assertEquals(25, ScreenMath.filledPixels(500, 1000, 50))
        assertEquals(50, ScreenMath.filledPixels(2000, 1000, 50))
        assertEquals(0, ScreenMath.filledPixels(10, 0, 50))
    }

    @Test
    fun FractionPixels_RoundsDownAndClamps() {
        assertEquals(0, ScreenMath.fractionPixels(0.01, 24))
        assertEquals(12, ScreenMath.fractionPixels(0.5, 24))
        assertEquals(24, ScreenMath.fractionPixels(3.0, 24))
        assertEquals(0, ScreenMath.fractionPixels(-1.0, 24))
        assertEquals(0, ScreenMath.fractionPixels(Double.NaN, 24))
    }

    @Test
    fun IsHovering_HalfOpenBounds() {
        assertTrue(ScreenMath.isHovering(10, 10, 10, 10, 5, 5))
        assertTrue(ScreenMath.isHovering(14, 14, 10, 10, 5, 5))
        assertFalse(ScreenMath.isHovering(15, 10, 10, 10, 5, 5))
    }

    @Test
    fun FormatAmount_GroupsThousands() {
        assertEquals("12,345", ScreenMath.formatAmount(12345))
    }
}

class HorizontalFacingTest {
    @Test
    fun Front_FacingPropertyOrDefault() {
        val furnace = Blocks.FURNACE.defaultBlockState().setValue(HorizontalFacing.FACING, Direction.EAST)
        assertEquals(Direction.EAST, HorizontalFacing.front(furnace))
        assertEquals(Direction.NORTH, HorizontalFacing.front(Blocks.STONE.defaultBlockState()))
    }

    @Test
    fun RotateAndMirror_TurnTheFront() {
        val east = Blocks.FURNACE.defaultBlockState().setValue(HorizontalFacing.FACING, Direction.EAST)
        assertEquals(Direction.SOUTH, HorizontalFacing.rotate(east, Rotation.CLOCKWISE_90).getValue(HorizontalFacing.FACING))
        assertEquals(Direction.WEST, HorizontalFacing.mirror(east, Mirror.FRONT_BACK).getValue(HorizontalFacing.FACING))
        assertEquals(Direction.EAST, HorizontalFacing.mirror(east, Mirror.LEFT_RIGHT).getValue(HorizontalFacing.FACING))
    }
}
