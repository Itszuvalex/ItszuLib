package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.TestableIFluidStack
import com.itszuvalex.itszulib.TestableIItemStack
import com.itszuvalex.itszulib.api.adapters.IItemStack
import net.minecraft.resources.Identifier
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private fun item(n: Int): Identifier = TestableIItemStack(n).item()
private fun fluid(n: Int): Identifier = TestableIFluidStack(n, 1).fluid()

/** Counts every slot read, to prove lookups do not scan. */
private class CountingStorage(size: Int) : ItemStorageArray(size) {
    var reads = 0
    override fun get(index: Int): IItemStack {
        reads++
        return super.get(index)
    }
}

class IndexedItemStorageTest : ItemStorageTestBase() {
    override fun storageWithSize(size: Int): IItemStorage = IndexedItemStorage(ItemStorageArray(size))

    @Test
    fun Index_FollowsWrites_SlotsCountsAndEmptySlots() {
        val storage = IndexedItemStorage(ItemStorageArray(6))
        storage.setSlot(1, TestableIItemStack(1, 10))
        storage.setSlot(4, TestableIItemStack(1, 5))
        storage.setSlot(2, TestableIItemStack(2, 3))
        Assertions.assertEquals(setOf(1, 4), storage.slotsOf(item(1)))
        Assertions.assertEquals(15, storage.count(item(1)))
        Assertions.assertEquals(setOf(item(1), item(2)), storage.items())
        Assertions.assertEquals(setOf(0, 3, 5), storage.emptySlots())
        storage.setSlot(1, IItemStack.Empty)
        Assertions.assertEquals(setOf(4), storage.slotsOf(item(1)))
        Assertions.assertTrue(1 in storage.emptySlots())
        storage.split(2, 3)
        Assertions.assertFalse(storage.contains(item(2)), "a split that empties a slot leaves the index")
    }

    @Test
    fun Lookups_DoNotScanTheStorage() {
        val inner = CountingStorage(1000)
        val storage = IndexedItemStorage(inner)
        storage.setSlot(500, TestableIItemStack(7, 3))
        storage.setSlot(900, TestableIItemStack(7, 4))
        inner.reads = 0
        Assertions.assertEquals(7, storage.count(item(7)))
        Assertions.assertTrue(storage.contains(item(7)))
        Assertions.assertTrue(inner.reads <= 2, "read ${inner.reads} slots")
    }

    @Test
    fun Insert_FillsMatchingSlotsThenEmpty() {
        val storage = IndexedItemStorage(ItemStorageArray(4))
        storage.setSlot(3, TestableIItemStack(1, 60))
        val left = storage.insert(TestableIItemStack(1, 10))
        Assertions.assertTrue(left.isEmpty())
        Assertions.assertEquals(64, storage.get(3).stackSize(), "the matching slot first")
        Assertions.assertEquals(6, storage.get(0).stackSize(), "then the first empty slot")
    }

    @Test
    fun Extract_TakesOneKindAcrossSlots() {
        val storage = IndexedItemStorage(ItemStorageArray(4))
        storage.setSlot(0, TestableIItemStack(1, 5))
        storage.setSlot(2, TestableIItemStack(1, 5))
        val taken = storage.extract(item(1), 8)
        Assertions.assertEquals(8, taken.stackSize())
        Assertions.assertEquals(2, storage.count(item(1)))
        Assertions.assertEquals(IItemStack.Empty, storage.extract(item(9), 1))
    }

    @Test
    fun Listeners_ToldWhenAnItemAppearsAndLeaves() {
        val storage = IndexedItemStorage(ItemStorageArray(4))
        val events = ArrayList<Pair<Identifier, Boolean>>()
        storage.addListener { key, present -> events += key to present }
        storage.setSlot(0, TestableIItemStack(1, 1))
        storage.setSlot(1, TestableIItemStack(1, 1))
        storage.setSlot(0, IItemStack.Empty)
        storage.setSlot(1, IItemStack.Empty)
        Assertions.assertEquals(listOf(item(1) to true, item(1) to false), events)
    }
}

class ItemStorageIndexTest {
    @Test
    fun Index_AsksOnlyStoragesHoldingTheItem() {
        val a = IndexedItemStorage(ItemStorageArray(4))
        val b = IndexedItemStorage(ItemStorageArray(4))
        val index = ItemStorageIndex()
        index.add(a)
        index.add(b)
        a.setSlot(0, TestableIItemStack(1, 5))
        b.setSlot(2, TestableIItemStack(1, 7))
        b.setSlot(3, TestableIItemStack(2, 1))
        Assertions.assertEquals(setOf(a, b), index.storagesWith(item(1)))
        Assertions.assertEquals(setOf(b), index.storagesWith(item(2)))
        Assertions.assertEquals(12L, index.count(item(1)))
        Assertions.assertEquals(listOf(a to 0, b to 2), index.find(item(1)).toList())
        Assertions.assertEquals(10, index.extract(item(1), 10).stackSize())
        Assertions.assertEquals(setOf(b), index.storagesWith(item(1)), "a ran out")
        index.remove(b)
        Assertions.assertTrue(index.items().isEmpty())
    }

    @Test
    fun Insert_PrefersStoragesHoldingTheItem() {
        val a = IndexedItemStorage(ItemStorageArray(4))
        val b = IndexedItemStorage(ItemStorageArray(4))
        val index = ItemStorageIndex().apply { add(a); add(b) }
        b.setSlot(1, TestableIItemStack(3, 1))
        index.insert(TestableIItemStack(3, 4))
        Assertions.assertEquals(5, b.count(item(3)))
        Assertions.assertEquals(0, a.count(item(3)))
    }
}

class IndexedFluidStorageTest {
    @Test
    fun FillAndDrain_UseTheIndex() {
        val storage = IndexedFluidStorage(FluidStorageArray(intArrayOf(1000, 1000, 1000)))
        Assertions.assertEquals(1500, storage.fill(TestableIFluidStack(1, 1500), true))
        Assertions.assertEquals(setOf(0, 1), storage.tanksOf(fluid(1)))
        Assertions.assertEquals(1000, storage.fill(TestableIFluidStack(2, 1200), true), "the rest of tank 1 is fluid 1's")
        Assertions.assertEquals(1500L, storage.amount(fluid(1)))
        Assertions.assertEquals(1500, storage.drain(TestableIFluidStack(1, 2000), true).amount())
        Assertions.assertFalse(storage.contains(fluid(1)))
        Assertions.assertEquals(setOf(0, 1), storage.emptyTanks())
    }

    @Test
    fun FluidIndex_AcrossStorages() {
        val a = IndexedFluidStorage(FluidStorageArray(intArrayOf(1000)))
        val b = IndexedFluidStorage(FluidStorageArray(intArrayOf(1000)))
        val index = FluidStorageIndex().apply { add(a); add(b) }
        b.fill(TestableIFluidStack(4, 300), true)
        Assertions.assertEquals(setOf(b), index.storagesWith(fluid(4)))
        Assertions.assertEquals(500, index.fill(TestableIFluidStack(4, 500), true))
        Assertions.assertEquals(800L, b.amount(fluid(4)), "the storage holding it first")
        Assertions.assertEquals(800, index.drain(TestableIFluidStack(4, 900), true).amount())
    }
}
