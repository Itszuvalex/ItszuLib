package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IItemStack
import com.itszuvalex.itszulib.{TestBase, TestItemStack}

/**
  * Created by Chris on 7/30/2016.
  */
class TestItemStorageArray extends TestBase {

  trait withStorage {
    val invSize = 10

    val array = new Array[IItemStack](invSize)
    val item0 = new TestItemStack(1, 1)
    val item1 = new TestItemStack(1, 5)
    val item2 = new TestItemStack(1, 10)
    val item3 = new TestItemStack(1, 1, 1)
    val item4 = new TestItemStack(1, 2, 2)
    val item5 = new TestItemStack(2)
    val item6 = new TestItemStack(2, 3, 1)
    array(0) = item0
    array(1) = item1
    array(2) = item2
    array(3) = item3
    array(4) = item4
    array(5) = item5
    array(6) = item6
    val storage = new ItemStorageArray(array)
  }

  "an item storage array" should {
    "be the size of its underlying storage" in new withStorage {
      storage.length shouldEqual invSize
    }
    "return the elements in its storage" in new withStorage {
      storage.indices.foreach { i =>
        storage(i) should be theSameInstanceAs array(i)
                              }
    }
    "update its underlying storage" in new withStorage {
      val index = 7
      storage(index) = new TestItemStack(5)
      array(index) should be theSameInstanceAs storage(index)
    }
    "when splitting" should {
      "on an empty slot return an empty IItemStack" in new withStorage {
        val ret = storage.split(7, 1)
        ret shouldBe 'Empty
      }
      "for less than the stack contains modify the underlying and return the split in a new stack" in new withStorage {
        val ret = storage.split(1, 2)
        ret should not be 'Empty
        ret.stackSize shouldBe 2
        storage(1) should not be 'Empty
        storage(1).stackSize shouldBe 3
      }
      "for exactly the stack return a copy of the stack and leave an empty stack in the storage" in new withStorage {
        val ret = storage.split(1, 5)
        ret should not be 'Empty
        ret.stackSize shouldBe 5
        storage(1) shouldBe 'Empty
      }
      "for more than the stack contains return a copy of the stack with as much as the stack had and leave an empty stack in the storage" in new withStorage {
        val ret = storage.split(1, 10)
        ret should not be 'Empty
        ret.stackSize shouldBe 5
        storage(1) shouldBe 'Empty
      }
    }
    "when inserting" should {
      "when inserting an empty itemstack modify nothing and return empty" in new withStorage {
        val ins  = IItemStack.Empty
        val copy = storage(1).copy()
        copy shouldNot be theSameInstanceAs storage(1)
        storage.insert(1, ins) shouldBe 'Empty
        copy.isItemEqual(storage(1)) shouldBe true
      }
      "when inserting a new itemstack into an empty slot insert the entire stack and return empty" in new withStorage {
        val ins = new TestItemStack(1, 2)
        storage(9) shouldBe 'Empty
        val ret = storage.insert(9, ins)
        ret shouldBe 'Empty
        storage(9) should be theSameInstanceAs ins
      }
      "when inserting an itemstack that matches in a slot with room, add the contents together and return empty" in new withStorage {
        val ins = new TestItemStack(1, 3)
        val cur = storage(1)
        cur.stackSize shouldBe 5
        ins.isItemEqual(cur) shouldBe true
        storage.insert(1, ins) shouldBe 'Empty
        cur.stackSize shouldBe 8
      }
      "when inserting an itemstack that matches in a slot with limited room, add until at max and return the remains" in new withStorage {
        val ins = new TestItemStack(1, 64)
        val cur = storage(1)
        cur.stackSize shouldBe 5
        ins.isItemEqual(cur) shouldBe true
        val ret = storage.insert(1, ins)
        ret should not be 'Empty
        ins.isItemEqual(ret) shouldBe true
        storage(1) should not be 'Empty
        storage(1).stackSize shouldBe 64
        ret.stackSize shouldBe 5
      }
      "when inserting an itemstack into an empty slot with greater than the amount of room, set the slot with as much as can fit return the remains" in new withStorage {
        val ins = new TestItemStack(1, 200)
        val cur = storage(9)
        cur shouldBe 'Empty
        val ret = storage.insert(9, ins)
        ret.isItemEqual(ins) shouldBe true
        ret.stackSize shouldBe (200 - 64)
        storage(9).isItemEqual(ins) shouldBe true
        storage(9).stackSize shouldBe storage.maxStackSize(9)
      }
      "when inserting an itemstack into a slot that contains a different itemstack, return the insert and modify nothing" in new withStorage {
        val ins      = new TestItemStack(3, 20)
        val insCopy  = ins.copy()
        val slotCopy = storage(1).copy()
        val ret      = storage.insert(1, ins)
        ret.isItemEqual(ins) shouldBe true
        ret.stackSize shouldBe ins.stackSize
        slotCopy.isItemEqual(storage(1)) shouldBe true
        slotCopy.stackSize shouldBe storage(1).stackSize
      }
    }
  }
}
