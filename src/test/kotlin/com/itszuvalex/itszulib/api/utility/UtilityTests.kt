package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.TestableLevel
import com.itszuvalex.itszulib.TestableLoc4
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.adapters.Module
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import org.joml.Vector3f
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

class DirectionUtilTest {
    private val horizontal = listOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)

    @Test
    fun GetHorizontalRelativeDirectionFromAbsolute_NorthFront_IsIdentity() {
        for (dir in Direction.entries) {
            Assertions.assertEquals(dir, DirectionUtil.getHorizontalRelativeDirectionFromAbsolute(dir, Direction.NORTH))
        }
    }

    @Test
    fun GetHorizontalRelativeDirectionFromAbsolute_UpDown_Unchanged() {
        for (front in horizontal) {
            Assertions.assertEquals(Direction.UP, DirectionUtil.getHorizontalRelativeDirectionFromAbsolute(Direction.UP, front))
            Assertions.assertEquals(Direction.DOWN, DirectionUtil.getHorizontalRelativeDirectionFromAbsolute(Direction.DOWN, front))
        }
    }

    @Test
    fun GetHorizontalRelativeDirectionFromAbsolute_FrontFace_IsRelativeNorth() {
        for (front in horizontal) {
            Assertions.assertEquals(Direction.NORTH, DirectionUtil.getHorizontalRelativeDirectionFromAbsolute(front, front))
        }
    }

    @Test
    fun GetHorizontalRelativeDirectionFromAbsolute_InvertsGetAbsoluteDirectionFromHorizontalRelative() {
        for (front in horizontal) {
            for (dir in Direction.entries) {
                val absolute = DirectionUtil.getAbsoluteDirectionFromHorizontalRelative(dir, front)
                Assertions.assertEquals(dir, DirectionUtil.getHorizontalRelativeDirectionFromAbsolute(absolute, front), "front=$front relative=$dir")
            }
        }
    }
}

class Loc4Test {
    @Test
    fun Equals_DifferentSubclassesSameLocation_EqualWithSameHash() {
        val pos = BlockPos(1, 2, 3)
        val anchored = Loc4.of(TestableLevel(TestableLoc4.DEFAULT_DIM), pos)
        val indirect: Loc4 = Loc4Indirect(TestableLoc4.DEFAULT_DIM, pos)
        Assertions.assertEquals(anchored, indirect)
        Assertions.assertEquals(indirect, anchored)
        Assertions.assertEquals(anchored.hashCode(), indirect.hashCode())
        Assertions.assertEquals(0, anchored.compareTo(indirect))
    }

    @Test
    fun Equals_DifferentSubclassesSameLocation_InterchangeableAsMapKeys() {
        val pos = BlockPos(1, 2, 3)
        val map = HashMap<Loc4, String>()
        map[Loc4.of(TestableLevel(TestableLoc4.DEFAULT_DIM), pos)] = "node"
        Assertions.assertEquals("node", map[Loc4Indirect(TestableLoc4.DEFAULT_DIM, pos)])
    }

    @Test
    fun Equals_DifferentDimensionOrPosition_NotEqual() {
        val pos = BlockPos(1, 2, 3)
        val loc = Loc4Indirect(TestableLoc4.DEFAULT_DIM, pos)
        Assertions.assertNotEquals(loc, Loc4Indirect(Identifier.parse("other"), pos))
        Assertions.assertNotEquals(loc, Loc4Indirect(TestableLoc4.DEFAULT_DIM, pos.above()))
        Assertions.assertFalse(loc.equals(pos))
    }

    @Test
    fun DistSqr_FarApartCoordinates_DoesNotOverflow() {
        val a = Loc4Indirect(TestableLoc4.DEFAULT_DIM, BlockPos(-30_000_000, 0, 0))
        val b = Loc4Indirect(TestableLoc4.DEFAULT_DIM, BlockPos(30_000_000, 0, 0))
        Assertions.assertEquals(3.6e15, a.distSqr(b), 1.0)
    }

    @Test
    fun Codec_RoundTrip_KeepsValue() {
        val loc = Loc4Indirect(TestableLoc4.DEFAULT_DIM, BlockPos(4, -5, 6))
        val encoded = Loc4.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, loc).getOrThrow()
        Assertions.assertEquals(loc, Loc4.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow())
    }
}

class LocationTrackerTest {
    private val dim = TestableLoc4.DEFAULT_DIM
    private fun loc(x: Int, y: Int, z: Int, d: Identifier = dim) = Loc4Indirect(d, BlockPos(x, y, z))

    @Test
    fun TrackLocation_ShouldTrackLocation() {
        val tracker = LocationTracker()
        Assertions.assertFalse(tracker.isLocationTracked(TestableLoc4.ORIGIN))
        tracker.trackLocation(TestableLoc4.ORIGIN)
        Assertions.assertTrue(tracker.isLocationTracked(TestableLoc4.ORIGIN))
    }

    @Test
    fun TrackLocation_ShouldNotReportUntrackedLocation() {
        val tracker = LocationTracker()
        tracker.trackLocation(TestableLoc4.ORIGIN)
        Assertions.assertTrue(tracker.isLocationTracked(TestableLoc4.ORIGIN))
        Assertions.assertFalse(tracker.isLocationTracked(loc(0, 0, 1)))
    }

    @Test
    fun RemoveLocation_ShouldTrackRemoveTrackedLocation() {
        val tracker = LocationTracker()
        tracker.trackLocation(TestableLoc4.ORIGIN)
        tracker.removeLocation(TestableLoc4.ORIGIN)
        Assertions.assertFalse(tracker.isLocationTracked(TestableLoc4.ORIGIN))
    }

    @Test
    fun Clear_ShouldUntrackAllLocations() {
        val tracker = LocationTracker()
        val loc1 = loc(0, 0, 1)
        tracker.trackLocation(TestableLoc4.ORIGIN)
        tracker.trackLocation(loc1)
        tracker.clear()
        Assertions.assertFalse(tracker.isLocationTracked(TestableLoc4.ORIGIN))
        Assertions.assertFalse(tracker.isLocationTracked(loc1))
    }

    @Test
    fun ClearDims_ShouldUntrackAllLocationsInDim() {
        val tracker = LocationTracker()
        val loc1 = loc(0, 0, 1)
        val loc2 = loc(0, 0, 1, Identifier.parse("stay"))
        listOf(TestableLoc4.ORIGIN, loc1, loc2).forEach(tracker::trackLocation)
        tracker.clearDim(dim)
        Assertions.assertFalse(tracker.isLocationTracked(TestableLoc4.ORIGIN))
        Assertions.assertFalse(tracker.isLocationTracked(loc1))
        Assertions.assertTrue(tracker.isLocationTracked(loc2))
    }

    @Test
    fun GetTrackedLocationsInDim_ReportsOnlyTrackedLocations() {
        val tracker = LocationTracker()
        val loc1 = loc(0, 0, 1)
        val loc2 = loc(0, 0, 1, Identifier.parse("stay"))
        listOf(TestableLoc4.ORIGIN, loc1, loc2).forEach(tracker::trackLocation)
        Assertions.assertEquals(setOf(TestableLoc4.ORIGIN, loc1), tracker.getTrackedLocationsInDim(dim).toSet())
        Assertions.assertEquals(setOf<Loc4>(loc2), tracker.getTrackedLocationsInDim(Identifier.parse("stay")).toSet())
    }

    @Test
    fun GetLocationsInRange_ReturnLocation() {
        val tracker = LocationTracker()
        tracker.trackLocation(TestableLoc4.ORIGIN)
        val found = tracker.getLocationsInRange(loc(0, 0, 0), 25f).firstOrNull()
        Assertions.assertEquals(TestableLoc4.ORIGIN, found)
    }

    @Test
    fun GetLocationsInRange_NotReturnLocationOutOfRange() {
        val tracker = LocationTracker()
        tracker.trackLocation(loc(0, 0, 30))
        Assertions.assertNull(tracker.getLocationsInRange(loc(0, 0, 0), 25f).firstOrNull())
    }

    @Test
    @Timeout(5, unit = TimeUnit.SECONDS)
    fun GetLocationsInRange_OnBlockCoords_ShouldNotDieOnBigRanges() {
        val tracker = LocationTracker()
        val loc1 = loc(0, 0, 3000000)
        tracker.trackLocation(TestableLoc4.ORIGIN)
        tracker.trackLocation(loc1)
        val locs = tracker.getLocationsInRange(TestableLoc4.ORIGIN, 5000000f).toList()
        Assertions.assertTrue(locs.all { it == TestableLoc4.ORIGIN || it == loc1 })
    }

    @Test
    @Timeout(5, unit = TimeUnit.SECONDS)
    fun GetLocationsInRange_OnPlayerCoords_ShouldNotDieOnBigRanges() {
        val tracker = LocationTracker()
        val loc1 = loc(0, 0, 3000000)
        tracker.trackLocation(TestableLoc4.ORIGIN)
        tracker.trackLocation(loc1)
        val locs = tracker.getLocationsInRange(dim, Vector3f(0f, 0f, 0f), 5000000f).toList()
        Assertions.assertTrue(locs.all { it == TestableLoc4.ORIGIN || it == loc1 })
    }

    @Test
    fun GetLocationsInRange_OnPlayerCoords_NotReturnLocationOutOfRange() {
        val tracker = LocationTracker()
        // Shares two coordinates with the query point, so a product of per-axis distances would be 0.
        tracker.trackLocation(loc(0, 0, 30))
        Assertions.assertEquals(0, tracker.getLocationsInRange(dim, Vector3f(0f, 0f, 0f), 25f).count())
    }

    @Test
    fun GetLocationsInRange_OnPlayerCoords_UseEuclideanDistance() {
        val tracker = LocationTracker()
        val inRange = loc(3, 4, 0) // distance 5
        val outOfRange = loc(3, 4, 12) // distance 13
        tracker.trackLocation(inRange)
        tracker.trackLocation(outOfRange)
        Assertions.assertEquals(listOf<Loc4>(inRange), tracker.getLocationsInRange(dim, Vector3f(0f, 0f, 0f), 5.5f).toList())
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ModuleCapabilityMapTestBase {
    lateinit var module: IModule<Int>

    @BeforeAll
    fun classSetup() {
        module = Module.registerModule(Identifier.fromNamespaceAndPath("itszulib_test", "module"), null)
    }

    @AfterAll
    fun classTeardown() = Module.clear()

    abstract fun getMap(): IMutableModuleCapabilityMap

    @Test
    fun AddModule_RegisterAndHaveModule() {
        val map = getMap()
        map.addModule(module) { 1 }
        Assertions.assertEquals(1, map.getModule(module, null))
        Direction.entries.forEach { Assertions.assertEquals(1, map.getModule(module, it)) }
    }

    @Test
    fun GetModule_ReturnTheCorrectModuleValueWhenAsked() {
        val map = getMap()
        map.addModule(module) { facing -> if (facing == null) null else 1 }
        Assertions.assertNull(map.getModule(module, null))
        Direction.entries.forEach { Assertions.assertEquals(1, map.getModule(module, it)) }
    }

    @Test
    fun InvalidateFrags_HidesModulesUntilRehydrated() {
        val map = getMap()
        map.addModule(module) { 1 }
        map.invalidateFrags()
        Assertions.assertNull(map.getModule(module, null))
        map.rehydrateFrags()
        Assertions.assertEquals(1, map.getModule(module, null))
    }
}

class ModuleCapabilityArrayListMapTest : ModuleCapabilityMapTestBase() {
    override fun getMap(): IMutableModuleCapabilityMap = ModuleCapabilityArrayListMap()
}

class ModuleCapabilityHashMapTest : ModuleCapabilityMapTestBase() {
    override fun getMap(): IMutableModuleCapabilityMap = ModuleCapabilityHashMap()
}

class ModuleTest {
    @AfterEach
    fun methodTeardown() = Module.clear()

    @Test
    fun RegisterModule_DuplicateId_Throws() {
        val id = Identifier.fromNamespaceAndPath("itszulib_test", "dup")
        Module.registerModule<Int>(id, null)
        Assertions.assertThrows(IllegalArgumentException::class.java) { Module.registerModule<Int>(id, null) }
    }

    @Test
    fun RegisterModule_SamePathDifferentNamespace_BothRegister() {
        val a = Module.registerModule<Int>(Identifier.fromNamespaceAndPath("mod_a", "colorable"), null)
        val b = Module.registerModule<Int>(Identifier.fromNamespaceAndPath("mod_b", "colorable"), null)
        Assertions.assertNotEquals(a.id, b.id)
        Assertions.assertTrue(Module.modules().contains(a))
        Assertions.assertTrue(Module.modules().contains(b))
    }
}
