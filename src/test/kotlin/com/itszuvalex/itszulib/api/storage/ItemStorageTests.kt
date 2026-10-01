package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.MCAssert
import com.itszuvalex.itszulib.TestIO
import com.itszuvalex.itszulib.TestableIItemStack
import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.utility.MCConstants
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.math.ceil
import kotlin.math.floor

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ItemStorageTestBase {
    @BeforeAll
    fun classSetup() = TestableIItemStack.overrideCodec()

    @AfterAll
    fun classTeardown() = TestableIItemStack.resetCodec()

    abstract fun storageWithSize(size: Int): IItemStorage

    inner class TestState {
        val item0 = TestableIItemStack(1, 1)
        val item1 = TestableIItemStack(1, 5)
        val item2 = TestableIItemStack(1, 10)
        val item3 = TestableIItemStack(1, 1, 1)
        val item4 = TestableIItemStack(1, 2, 2)
        val item5 = TestableIItemStack(2)
        val item6 = TestableIItemStack(2, 3, 1)
        val storage = storageWithSize(SIZE).also {
            it.setSlot(0, item0)
            it.setSlot(1, item1)
            it.setSlot(2, item2)
            it.setSlot(3, item3)
            it.setSlot(4, item4)
            it.setSlot(5, item5)
            it.setSlot(6, item6)
        }
        val testLength = SIZE
    }

    private fun state() = TestState()

    private fun total(storage: IItemStorage) = (0 until storage.size()).sumOf { storage.get(it).stackSize() }

    // 1.12.2 wrote back a copy of the source taken before the insert, so moving a slot into itself (or into a view of
    // the same slot) replaced the merged stack with the leftover and destroyed the items.
    @Test
    fun TransferSlotIntoStorageSlot_SameSlot_KeepsItems() {
        val state = state()
        val remaining = state.storage.transferSlotIntoStorageSlot(2, state.storage, 2, 10)
        Assertions.assertEquals(10, state.storage.get(2).stackSize())
        Assertions.assertEquals(0, remaining)
    }

    @Test
    fun TransferSlotIntoStorage_IntoItself_KeepsItems() {
        val storage = storageWithSize(1)
        storage.setSlot(0, TestableIItemStack(1, 10))
        storage.transferSlotIntoStorage(0, storage, 10)
        Assertions.assertEquals(10, storage.get(0).stackSize())
    }

    @Test
    fun TransferSlotIntoStorageSlot_AliasingView_KeepsItems() {
        val storage = storageWithSize(2)
        storage.setSlot(1, TestableIItemStack(1, 10))
        val view = ItemStorageSlice(storage, intArrayOf(1))
        storage.transferSlotIntoStorageSlot(1, view, 0, 10)
        Assertions.assertEquals(10, total(storage))
    }

    // 1.12.2 subtracted a negative amount, growing the slot.
    @Test
    fun Split_NegativeAmount_ChangesNothing() {
        val state = state()
        MCAssert.assertIItemStackEmpty(state.storage.split(2, -5))
        Assertions.assertEquals(10, state.storage.get(2).stackSize())
    }

    @Test
    fun Split_Zero_ReturnsEmpty() {
        val state = state()
        MCAssert.assertIItemStackEmpty(state.storage.split(2, 0))
        Assertions.assertEquals(10, state.storage.get(2).stackSize())
    }

    @Test
    fun SetSlot_SetsSlot() {
        val storage = storageWithSize(1)
        MCAssert.assertIItemStackEmpty(storage.get(0))
        storage.setSlot(0, TestableIItemStack(1, 3))
        MCAssert.assertIItemStackNotEmpty(storage.get(0))
        Assertions.assertEquals(3, storage.get(0).stackSize())
    }

    @Test
    fun Size_Always_EqualsArrayLength() {
        val state = state()
        Assertions.assertEquals(state.testLength, state.storage.size())
    }

    @Test
    fun Split_OnEmpty_ReturnEmpty() {
        MCAssert.assertIItemStackEmpty(state().storage.split(7, 1))
    }

    @Test
    fun Split_OnLessThanStack_ModifiesAndReturns() {
        val state = state()
        val ret = state.storage.split(1, 2)
        MCAssert.assertIItemStackNotEmpty(ret)
        Assertions.assertEquals(2, ret.stackSize())
        MCAssert.assertIItemStackNotEmpty(state.storage.get(1))
        Assertions.assertEquals(3, state.storage.get(1).stackSize())
    }

    @Test
    fun Split_OnExactStack_ModifiesEmptiesAndReturns() {
        val state = state()
        val ret = state.storage.split(1, 5)
        MCAssert.assertIItemStackNotEmpty(ret)
        Assertions.assertEquals(5, ret.stackSize())
        MCAssert.assertIItemStackEmpty(state.storage.get(1))
    }

    @Test
    fun Split_OnMore_ModifiesEmptiesAndReturnsContents() {
        val state = state()
        val ret = state.storage.split(1, 10)
        MCAssert.assertIItemStackNotEmpty(ret)
        Assertions.assertEquals(5, ret.stackSize())
        MCAssert.assertIItemStackEmpty(state.storage.get(1))
    }

    @Test
    fun Insert_EmptyItemStack_ModifyNothingReturnEmpty() {
        val state = state()
        val copy = state.storage.get(1).copy()
        Assertions.assertNotSame(copy, state.storage.get(1))
        MCAssert.assertIItemStackEmpty(state.storage.insert(1, IItemStack.Empty))
        Assertions.assertTrue(copy.isItemEqual(state.storage.get(1)))
    }

    @Test
    fun Insert_ItemIntoEmpty_InsertWholeItemReturnEmpty() {
        val state = state()
        val ins = TestableIItemStack(1, 2)
        MCAssert.assertIItemStackEmpty(state.storage.get(9))
        MCAssert.assertIItemStackEmpty(state.storage.insert(9, ins))
        Assertions.assertTrue(state.storage.get(9).isItemEqual(ins))
        Assertions.assertEquals(ins.stackSize(), state.storage.get(9).stackSize())
    }

    @Test
    fun Insert_ItemIntoMatchingItemStackWithRoom_ModifyReturnEmpty() {
        val state = state()
        val ins = TestableIItemStack(1, 3)
        Assertions.assertEquals(5, state.storage.get(1).stackSize())
        Assertions.assertTrue(ins.isItemEqual(state.storage.get(1)))
        MCAssert.assertIItemStackEmpty(state.storage.insert(1, ins))
        Assertions.assertEquals(8, state.storage.get(1).stackSize())
    }

    @Test
    fun Insert_ItemIntoMatchingItemLimitedRoom_FillReturnRemains() {
        val state = state()
        val ins = TestableIItemStack(1, 63)
        val ret = state.storage.insert(1, ins)
        MCAssert.assertIItemStackNotEmpty(ret)
        Assertions.assertTrue(ins.isItemEqual(ret))
        Assertions.assertEquals(MCConstants.ITEMSTACK_MAX, state.storage.get(1).stackSize())
        Assertions.assertEquals(4, ret.stackSize())
    }

    @Test
    fun Insert_ItemIntoSlotWithLimitedRoom_FillUntilMaxReturnRemaining() {
        val state = state()
        val ins = TestableIItemStack(1, 200)
        MCAssert.assertIItemStackEmpty(state.storage.get(9))
        val ret = state.storage.insert(9, ins)
        Assertions.assertTrue(ret.isItemEqual(ins))
        Assertions.assertEquals(200 - MCConstants.ITEMSTACK_MAX, ret.stackSize())
        Assertions.assertTrue(state.storage.get(9).isItemEqual(ins))
        Assertions.assertEquals(state.storage.maxStackSize(9), state.storage.get(9).stackSize())
    }

    @Test
    fun Insert_StackIntoSlotContainingDifferentItem_ReturnInsertModifyNothing() {
        val state = state()
        val ins = TestableIItemStack(3, 20)
        val slotCopy = state.storage.get(1).copy()
        val ret = state.storage.insert(1, ins)
        Assertions.assertTrue(ret.isItemEqual(ins))
        Assertions.assertEquals(ins.stackSize(), ret.stackSize())
        Assertions.assertTrue(slotCopy.isItemEqual(state.storage.get(1)))
        Assertions.assertEquals(slotCopy.stackSize(), state.storage.get(1).stackSize())
    }

    @Test
    fun TransferIntoStorage_OneStackInFirstRoomInSecond_EmptyFirstInventoryInsertIntoSecond() {
        val emptying = storageWithSize(1).also { it.setSlot(0, TestableIItemStack(1, 1)) }
        val filling = storageWithSize(1).also { it.setSlot(0, IItemStack.Empty) }
        val ret = emptying.transferIntoStorage(filling, 1)
        MCAssert.assertIItemStackEmpty(emptying.get(0))
        Assertions.assertEquals(1, filling.get(0).stackSize())
        Assertions.assertEquals(0, ret)
    }

    @Test
    fun TransferIntoStorage_TransferSomeOfEntireStack_ReduceFirstInsertSecond() {
        val emptying = storageWithSize(1).also { it.setSlot(0, TestableIItemStack(1, 2)) }
        val filling = storageWithSize(1)
        val ret = emptying.transferIntoStorage(filling, 1)
        Assertions.assertEquals(1, emptying.get(0).stackSize())
        Assertions.assertEquals(1, filling.get(0).stackSize())
        Assertions.assertEquals(0, ret)
    }

    @Test
    fun TransferIntoStorage_TransferRequestMoreThanExists_EmptyFirstInsertSecondReturnRemaining() {
        val emptying = storageWithSize(1).also { it.setSlot(0, TestableIItemStack(1, 1)) }
        val filling = storageWithSize(1)
        val ret = emptying.transferIntoStorage(filling, 2)
        MCAssert.assertIItemStackEmpty(emptying.get(0))
        Assertions.assertEquals(1, filling.get(0).stackSize())
        Assertions.assertEquals(1, ret)
    }

    @Test
    fun TransferIntoStorage_MultipleOfItemIntoMostlyFilledStack_OverflowIntoSecondSlot() {
        val emptying = storageWithSize(1).also { it.setSlot(0, TestableIItemStack(1, 5)) }
        val filling = storageWithSize(2).also {
            it.setSlot(0, TestableIItemStack(1, 63))
            it.setSlot(1, TestableIItemStack(1, 1))
        }
        val ret = emptying.transferIntoStorage(filling, 2)
        Assertions.assertEquals(3, emptying.get(0).stackSize())
        Assertions.assertEquals(MCConstants.ITEMSTACK_MAX, filling.get(0).stackSize())
        Assertions.assertEquals(2, filling.get(1).stackSize())
        Assertions.assertEquals(0, ret)
    }

    @Test
    fun TransferIntoStorage_TransferMultipleTypesOfItems() {
        val emptying = storageWithSize(2).also {
            it.setSlot(0, TestableIItemStack(0))
            it.setSlot(1, TestableIItemStack(1))
        }
        val filling = storageWithSize(2)
        val ret = emptying.transferIntoStorage(filling, 2)
        MCAssert.assertIItemStackEmpty(emptying.get(0))
        MCAssert.assertIItemStackEmpty(emptying.get(1))
        Assertions.assertEquals(0, (filling.get(0) as TestableIItemStack).testItem)
        Assertions.assertEquals(1, (filling.get(1) as TestableIItemStack).testItem)
        Assertions.assertEquals(0, ret)
    }

    @Test
    fun TransferIntoStorage_MergeItemsTogether() {
        val emptying = storageWithSize(2).also {
            it.setSlot(0, TestableIItemStack(1))
            it.setSlot(1, TestableIItemStack(1))
        }
        val filling = storageWithSize(1)
        val ret = emptying.transferIntoStorage(filling, 2)
        MCAssert.assertIItemStackEmpty(emptying.get(0))
        MCAssert.assertIItemStackEmpty(emptying.get(1))
        Assertions.assertEquals(2, filling.get(0).stackSize())
        Assertions.assertEquals(0, ret)
    }

    @Test
    fun TransferIntoStorage_PrioritizeMatchingOverEmptySlot() {
        val emptying = storageWithSize(1).also { it.setSlot(0, TestableIItemStack(1, 1)) }
        val filling = storageWithSize(2).also { it.setSlot(1, TestableIItemStack(1, 1)) }
        val ret = emptying.transferIntoStorage(filling, 1)
        MCAssert.assertIItemStackEmpty(emptying.get(0))
        MCAssert.assertIItemStackEmpty(filling.get(0))
        Assertions.assertEquals(2, filling.get(1).stackSize())
        Assertions.assertEquals(0, ret)
    }

    @Test
    fun TransferIntoStorage_MoveNothingIfNoRoom() {
        val emptying = storageWithSize(1).also { it.setSlot(0, TestableIItemStack(1, 1)) }
        val filling = storageWithSize(1).also { it.setSlot(0, TestableIItemStack(2, 1)) }
        val ret = emptying.transferIntoStorage(filling, 1)
        Assertions.assertEquals(1, emptying.get(0).stackSize())
        Assertions.assertEquals(1, filling.get(0).stackSize())
        Assertions.assertEquals(1, ret)
    }

    @Test
    fun SerializeDeserializeNBT_ShouldWork() {
        val state = state()
        val storage2 = ItemStorageArray(state.testLength)
        val nbt = TestIO.write(state.storage::serialize)
        storage2.deserialize(TestIO.read(nbt))
        for (i in 0 until state.storage.size()) {
            Assertions.assertEquals(state.storage.get(i).isEmpty(), storage2.get(i).isEmpty())
            if (!state.storage.get(i).isEmpty()) {
                Assertions.assertEquals((state.storage.get(i) as TestableIItemStack).testItem, (storage2.get(i) as TestableIItemStack).testItem)
                Assertions.assertEquals(state.storage.get(i).stackSize(), storage2.get(i).stackSize())
                Assertions.assertEquals(state.storage.get(i).damage(), storage2.get(i).damage())
                Assertions.assertNotSame(state.storage.get(i), storage2.get(i))
            }
        }
    }

    @Test
    fun Deserialize_IntoOccupiedStorage_ClearSlotsMissingFromData() {
        val state = state()
        MCAssert.assertIItemStackEmpty(state.storage.get(9))
        val target = storageWithSize(state.testLength)
        target.setSlot(9, TestableIItemStack(4, 7))
        target.deserialize(TestIO.read(TestIO.write(state.storage::serialize)))
        MCAssert.assertIItemStackEmpty(target.get(9))
        Assertions.assertEquals(state.storage.get(1).stackSize(), target.get(1).stackSize())
    }

    @Test
    fun Insert_SlotAlreadyOverInsertedStackLimit_InsertNothingReturnStack() {
        val storage = storageWithSize(1)
        storage.setSlot(0, TestableIItemStack(1, 10))
        val ins = TestableIItemStack(1, 3).also { it.testStackMax = 4 }
        Assertions.assertTrue(ins.isItemEqual(storage.get(0)))
        val ret = storage.insert(0, ins)
        Assertions.assertEquals(10, storage.get(0).stackSize())
        Assertions.assertEquals(3, ret.stackSize())
        Assertions.assertTrue(ret.isItemEqual(ins))
    }

    @Test
    fun Insert_ItemIntoEmpty_CallerMutationDoesNotAffectStorage() {
        val storage = storageWithSize(1)
        val ins = TestableIItemStack(1, 5)
        storage.insert(0, ins)
        ins.setStackSize(1)
        Assertions.assertEquals(5, storage.get(0).stackSize())
    }

    companion object {
        const val SIZE = 10
    }
}

class ItemStorageArrayTest : ItemStorageTestBase() {
    override fun storageWithSize(size: Int): IItemStorage = ItemStorageArray(size)

    @Test
    fun Listener_SetSlotAndInsertNotify_QuietDoesNot() {
        var changes = 0
        val storage = ItemStorageArray(2) { changes++ }
        storage.setSlotQuietly(0, TestableIItemStack(1, 1))
        Assertions.assertEquals(0, changes)
        storage.setSlot(0, TestableIItemStack(1, 2))
        Assertions.assertEquals(1, changes)
        storage.insert(1, TestableIItemStack(1, 3))
        Assertions.assertEquals(2, changes)
        storage.setChanged()
        Assertions.assertEquals(3, changes)
    }

    @Test
    fun Slice_ForwardsQuietAndSetChanged() {
        var changes = 0
        val slice = ItemStorageSlice(ItemStorageArray(2) { changes++ }, intArrayOf(1))
        slice.setSlotQuietly(0, TestableIItemStack(1, 1))
        Assertions.assertEquals(0, changes)
        slice.setChanged()
        Assertions.assertEquals(1, changes)
    }
}

class ItemStorageAggregateTest : ItemStorageTestBase() {
    override fun storageWithSize(size: Int): IItemStorage {
        val size1 = ceil(size / 2f).toInt()
        val size2 = floor(size / 2f).toInt()
        val storages: Array<IItemStorage> =
            if (size <= 1) arrayOf(ItemStorageArray(size)) else arrayOf(ItemStorageArray(size1), ItemStorageArray(size2))
        return ItemStorageAggregate(storages)
    }
}

class ItemStorageNBTTest : ItemStorageTestBase() {
    override fun storageWithSize(size: Int): IItemStorage = ItemStorageNBT(CompoundTag(), size)
}

class ItemStorageSliceTest : ItemStorageTestBase() {
    override fun storageWithSize(size: Int): IItemStorage = ItemStorageSlice(ItemStorageArray(size * 2), (1..size).toList().toIntArray())
}
