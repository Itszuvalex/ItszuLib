package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.TestIO
import com.itszuvalex.itszulib.TestableIFluidStack
import com.itszuvalex.itszulib.api.adapters.IFluidStack
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private fun fluid(id: Int, amount: Int) = TestableIFluidStack(id, amount)

private fun assertTank(storage: IFluidStorage, index: Int, id: Int, amount: Int) {
    val stack = storage.get(index)
    Assertions.assertEquals(amount, stack.amount(), "tank $index amount")
    if (amount > 0) Assertions.assertEquals(id, (stack as TestableIFluidStack).testFluid, "tank $index fluid")
}

private fun assertEmptyTank(storage: IFluidStorage, index: Int) =
    Assertions.assertTrue(storage.get(index).isEmpty(), "tank $index should be empty, was ${storage.get(index)}")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FluidStorageArrayTest {
    @BeforeAll
    fun classSetup() = TestableIFluidStack.overrideCodec()

    @AfterAll
    fun classTeardown() = TestableIFluidStack.resetCodec()

    @Test
    fun Fill_Empty_FillsFirstEmptyTank() {
        val storage = FluidStorageArray(3, 1000)
        Assertions.assertEquals(400, storage.fill(fluid(1, 400), true))
        assertTank(storage, 0, 1, 400)
        assertEmptyTank(storage, 1)
    }

    @Test
    fun Fill_PrefersTanksAlreadyHoldingTheFluid() {
        val storage = FluidStorageArray(3, 1000)
        storage.set(2, fluid(1, 100))
        Assertions.assertEquals(300, storage.fill(fluid(1, 300), true))
        assertEmptyTank(storage, 0)
        assertTank(storage, 2, 1, 400)
    }

    @Test
    fun Fill_OverflowsMatchingTankIntoEmptyTanks() {
        val storage = FluidStorageArray(3, 1000)
        storage.set(1, fluid(1, 900))
        Assertions.assertEquals(600, storage.fill(fluid(1, 600), true))
        assertTank(storage, 1, 1, 1000)
        assertTank(storage, 0, 1, 500)
        assertEmptyTank(storage, 2)
    }

    @Test
    fun Fill_SkipsTanksHoldingOtherFluids() {
        val storage = FluidStorageArray(2, 1000)
        storage.set(0, fluid(2, 10))
        Assertions.assertEquals(500, storage.fill(fluid(1, 500), true))
        assertTank(storage, 0, 2, 10)
        assertTank(storage, 1, 1, 500)
    }

    @Test
    fun Fill_Full_ReturnsPartial() {
        val storage = FluidStorageArray(1, 1000)
        storage.set(0, fluid(2, 1000))
        Assertions.assertEquals(0, storage.fill(fluid(1, 500), true))
        Assertions.assertEquals(0, storage.fill(fluid(2, 500), true))
        val partial = FluidStorageArray(1, 1000)
        Assertions.assertEquals(1000, partial.fill(fluid(1, 1500), true))
    }

    @Test
    fun Fill_Simulated_DoesNotChangeTanks() {
        var changes = 0
        val storage = FluidStorageArray(2, 1000) { changes++ }
        storage.set(0, fluid(1, 900))
        changes = 0
        Assertions.assertEquals(1100, storage.fill(fluid(1, 1100), false))
        assertTank(storage, 0, 1, 900)
        assertEmptyTank(storage, 1)
        Assertions.assertEquals(0, changes)
    }

    @Test
    fun Fill_EmptyResource_FillsNothing() {
        val storage = FluidStorageArray(1, 1000)
        Assertions.assertEquals(0, storage.fill(IFluidStack.Empty, true))
        Assertions.assertEquals(0, storage.fill(fluid(1, 0), true))
    }

    @Test
    fun Fill_DoesNotAliasTheResource() {
        val storage = FluidStorageArray(1, 1000)
        val resource = fluid(1, 100)
        storage.fill(resource, true)
        resource.testAmount = 999
        assertTank(storage, 0, 1, 100)
    }

    @Test
    fun Fill_RespectsCanFill() {
        val storage = object : FluidStorageArray(2, 1000) {
            override fun canFill(index: Int): Boolean = index != 0
        }
        Assertions.assertEquals(100, storage.fill(fluid(1, 100), true))
        assertEmptyTank(storage, 0)
        assertTank(storage, 1, 1, 100)
    }

    // 1.12.2 picked the first tank to drain by canDrain alone, so a tank whose fluid canDrainFluidType refuses was
    // drained anyway.
    @Test
    fun DrainAmount_RespectsCanDrainFluidType() {
        val storage = object : FluidStorageArray(2, 1000) {
            override fun canDrainFluidType(index: Int, resource: IFluidStack): Boolean =
                (resource as TestableIFluidStack).testFluid != 1 && super.canDrainFluidType(index, resource)
        }
        storage.set(0, fluid(1, 100))
        storage.set(1, fluid(2, 100))
        val drained = storage.drain(50, true) as TestableIFluidStack
        Assertions.assertEquals(2, drained.testFluid)
        Assertions.assertEquals(50, drained.testAmount)
        assertTank(storage, 0, 1, 100)
        assertTank(storage, 1, 2, 50)
    }

    @Test
    fun DrainAmount_NothingDrainable_ReturnsEmpty() {
        val storage = object : FluidStorageArray(1, 1000) {
            override fun canDrainFluidType(index: Int, resource: IFluidStack): Boolean = false
        }
        storage.set(0, fluid(1, 100))
        Assertions.assertTrue(storage.drain(50, true).isEmpty())
        assertTank(storage, 0, 1, 100)
    }

    @Test
    fun Fill_UsesPerTankCapacity() {
        val storage = FluidStorageArray(intArrayOf(100, 500))
        Assertions.assertEquals(600, storage.fill(fluid(1, 1000), true))
        assertTank(storage, 0, 1, 100)
        assertTank(storage, 1, 1, 500)
    }

    @Test
    fun DrainResource_DrainsOnlyThatFluid_AcrossTanks() {
        val storage = FluidStorageArray(3, 1000)
        storage.set(0, fluid(2, 500))
        storage.set(1, fluid(1, 200))
        storage.set(2, fluid(1, 300))
        val drained = storage.drain(fluid(1, 400), true) as TestableIFluidStack
        Assertions.assertEquals(1, drained.testFluid)
        Assertions.assertEquals(400, drained.amount())
        assertTank(storage, 0, 2, 500)
        assertEmptyTank(storage, 1)
        assertTank(storage, 2, 1, 100)
    }

    @Test
    fun DrainResource_Missing_ReturnsEmpty() {
        val storage = FluidStorageArray(1, 1000)
        storage.set(0, fluid(2, 500))
        Assertions.assertTrue(storage.drain(fluid(1, 100), true).isEmpty())
        Assertions.assertTrue(storage.drain(IFluidStack.Empty, true).isEmpty())
        assertTank(storage, 0, 2, 500)
    }

    @Test
    fun DrainResource_Simulated_DoesNotChangeTanks() {
        val storage = FluidStorageArray(2, 1000)
        storage.set(0, fluid(1, 200))
        storage.set(1, fluid(1, 300))
        Assertions.assertEquals(500, storage.drain(fluid(1, 1000), false).amount())
        assertTank(storage, 0, 1, 200)
        assertTank(storage, 1, 1, 300)
    }

    @Test
    fun DrainMax_TakesFirstFluid_ThenTopsUpFromSameFluid() {
        val storage = FluidStorageArray(4, 1000)
        storage.set(1, fluid(1, 100))
        storage.set(2, fluid(2, 500))
        storage.set(3, fluid(1, 300))
        val drained = storage.drain(250, true) as TestableIFluidStack
        Assertions.assertEquals(1, drained.testFluid)
        Assertions.assertEquals(250, drained.amount())
        assertEmptyTank(storage, 1)
        assertTank(storage, 2, 2, 500)
        assertTank(storage, 3, 1, 150)
    }

    /**
     * Simulating must not count the first tank twice: it still holds what was "drained".
     */
    @Test
    fun DrainMax_Simulated_DoesNotCountFirstTankTwice() {
        val storage = FluidStorageArray(2, 1000)
        storage.set(0, fluid(1, 100))
        storage.set(1, fluid(1, 50))
        Assertions.assertEquals(150, storage.drain(1000, false).amount())
        assertTank(storage, 0, 1, 100)
        assertTank(storage, 1, 1, 50)
        Assertions.assertEquals(150, storage.drain(1000, true).amount())
        Assertions.assertTrue(storage.isEmpty())
    }

    @Test
    fun DrainMax_SkipsUndrainableTanks() {
        val storage = object : FluidStorageArray(2, 1000) {
            override fun canDrain(index: Int): Boolean = index != 0
        }
        storage.set(0, fluid(1, 100))
        storage.set(1, fluid(2, 100))
        val drained = storage.drain(50, true) as TestableIFluidStack
        Assertions.assertEquals(2, drained.testFluid)
        assertTank(storage, 0, 1, 100)
        assertTank(storage, 1, 2, 50)
    }

    @Test
    fun DrainMax_NothingOrNonPositive_ReturnsEmpty() {
        val storage = FluidStorageArray(1, 1000)
        Assertions.assertTrue(storage.drain(100, true).isEmpty())
        storage.set(0, fluid(1, 100))
        Assertions.assertTrue(storage.drain(0, true).isEmpty())
        assertTank(storage, 0, 1, 100)
    }

    @Test
    fun Set_EmptyStack_StoresEmpty() {
        val storage = FluidStorageArray(1, 1000)
        storage.set(0, fluid(1, 0))
        Assertions.assertSame(IFluidStack.Empty, storage.get(0))
    }

    @Test
    fun Set_NotifiesListener_SetQuietlyDoesNot() {
        var changes = 0
        val storage = FluidStorageArray(1, 1000) { changes++ }
        storage.set(0, fluid(1, 1))
        Assertions.assertEquals(1, changes)
        storage.setQuietly(0, fluid(1, 2))
        Assertions.assertEquals(1, changes)
        storage.setChanged()
        Assertions.assertEquals(2, changes)
    }

    @Test
    fun Serialization_RoundTrips_AndOmitsEmptyTanks() {
        val storage = FluidStorageArray(3, 1000)
        storage.set(0, fluid(1, 100))
        storage.set(2, fluid(2, 300))
        val tag = TestIO.write(storage::serialize)
        Assertions.assertTrue(tag.contains("0"))
        Assertions.assertFalse(tag.contains("1"))
        Assertions.assertTrue(tag.contains("2"))

        val loaded = FluidStorageArray(3, 1000)
        loaded.set(1, fluid(5, 5))
        loaded.deserialize(TestIO.read(tag))
        assertTank(loaded, 0, 1, 100)
        assertEmptyTank(loaded, 1)
        assertTank(loaded, 2, 2, 300)
    }

    @Test
    fun Empty_HasNoTanks() {
        Assertions.assertEquals(0, IFluidStorage.Empty.size())
        Assertions.assertEquals(0, IFluidStorage.Empty.fill(fluid(1, 100), true))
        Assertions.assertTrue(IFluidStorage.Empty.drain(100, true).isEmpty())
        Assertions.assertTrue(IFluidStorage.Empty.isEmpty())
    }
}

class FluidStorageViewTest {
    @Test
    fun Slice_MapsIndices_AndFillsOnlyItsTanks() {
        var changes = 0
        val backing = FluidStorageArray(intArrayOf(100, 200, 300)) { changes++ }
        val slice = FluidStorageSlice(backing, intArrayOf(2, 0))
        Assertions.assertEquals(2, slice.size())
        Assertions.assertEquals(300, slice.capacity(0))
        Assertions.assertEquals(400, slice.fill(fluid(1, 1000), true))
        assertTank(backing, 2, 1, 300)
        assertTank(backing, 0, 1, 100)
        assertEmptyTank(backing, 1)
        Assertions.assertTrue(changes > 0)
        changes = 0
        slice.setQuietly(1, fluid(1, 1))
        Assertions.assertEquals(0, changes)
        assertTank(backing, 0, 1, 1)
    }

    @Test
    fun Dynamic_ForwardsToCurrentStorage() {
        val a = FluidStorageArray(1, 100)
        val b = FluidStorageArray(2, 100)
        var current: IFluidStorage = a
        val dynamic = DynamicIFluidStorage { current }
        Assertions.assertEquals(1, dynamic.size())
        dynamic.fill(fluid(1, 50), true)
        assertTank(a, 0, 1, 50)
        current = b
        Assertions.assertEquals(2, dynamic.size())
        Assertions.assertEquals(150, dynamic.fill(fluid(1, 150), true))
        assertTank(b, 1, 1, 50)
        Assertions.assertEquals(150, dynamic.drain(1000, true).amount())
        assertTank(a, 0, 1, 50)
    }
}
