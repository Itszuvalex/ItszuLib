package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.{TestBase, TestableItemStack}

import scala.collection.mutable.ArrayBuffer

class TestItemStorageAggregate extends TestBase {

  trait withStorage {
    val storage0 = new ItemStorageArray(1)
    val storage1 = new ItemStorageArray(1)
    val item0    = new TestableItemStack(1, 1)
    val item1    = new TestableItemStack(2, 10)
    val storage  = new ItemStorageAggregate(ArrayBuffer(storage0, storage1))
  }

  "ItemStorageAggregate" should {
    "have size equal to all of its contents" in new withStorage {
      storage.length shouldBe (storage0.length + storage1.length)
    }
    "return the values from its contents" in new withStorage {
      storage(0) should be theSameInstanceAs storage0(0)
      storage(1) should be theSameInstanceAs storage1(0)
    }
    "update the values of its contents" in new withStorage {
      val updateItem0 = new TestableItemStack(3, 3)
      storage(0) = updateItem0
      storage0(0) should be theSameInstanceAs updateItem0
      val updateItem1 = new TestableItemStack(4, 4)
      storage(1) = updateItem1
      storage1(0) should be theSameInstanceAs updateItem1
    }
  }
}
