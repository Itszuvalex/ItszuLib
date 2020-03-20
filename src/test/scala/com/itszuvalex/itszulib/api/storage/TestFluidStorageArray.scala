package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import com.itszuvalex.itszulib.{TestBase, TestableFluidStack}

class TestFluidStorageArray extends TestBase {

  trait withStorage {
    val invSize = 10

    val array = new Array[IFluidStack](invSize)
    val item0 = new TestableFluidStack(1, 1)
    val item1 = new TestableFluidStack(1, 5)
    val item3 = new TestableFluidStack(1, 1)
    val item5 = new TestableFluidStack(2, 1)
    val item6 = new TestableFluidStack(2, 3)
    array(0) = item0
    array(1) = item1
    array(3) = item3
    array(5) = item5
    array(6) = item6
    var capacityMapper: Int => Int        = (_ => Int.MaxValue)
    val storage       : FluidStorageArray = new FluidStorageArray(array) {
      override def capacity(index: Int): Int = capacityMapper(index)
    }
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
      storage(index) = new TestableFluidStack(5)
      array(index) should be theSameInstanceAs storage(index)
    }
    "when filling" should {
      "fill the first matching slot" in new withStorage {
        val toFill = new TestableFluidStack(1, 50)
        val ret    = storage.fill(toFill, true)
        ret shouldBe toFill.amount
        toFill.amount shouldBe 50
        storage(0).amount shouldBe 51
        storage(1).amount shouldBe 5
        storage(2) shouldBe 'Empty
        storage(3).amount shouldBe 1
        storage(4) shouldBe 'Empty
        storage(5).amount shouldBe 1
        storage(6).amount shouldBe 3
      }
      "overflow to more matching slots" in new withStorage {
        item0.fluidMax = 10
        item1.fluidMax = 10
        val toFill = new TestableFluidStack(1, 50)
        val ret    = storage.fill(toFill, true)
        ret shouldBe toFill.amount
        toFill.amount shouldBe 50
        storage(0).amount shouldBe 10
        storage(1).amount shouldBe 10
        storage(2) shouldBe 'Empty
        storage(3).amount shouldBe (1 + (50 - (10 - 1) - (10 - 5)))
        storage(4) shouldBe 'Empty
        storage(5).amount shouldBe 1
        storage(6).amount shouldBe 3
      }
      "overflow to the first empty tank" in new withStorage {
        item0.fluidMax = 10
        item1.fluidMax = 10
        item3.fluidMax = 10
        val toFill = new TestableFluidStack(1, 50)
        val ret    = storage.fill(toFill, true)
        ret shouldBe toFill.amount
        toFill.amount shouldBe 50
        storage(0).amount shouldBe 10
        storage(1).amount shouldBe 10
        storage(2).amount shouldBe (50 - (10 - 1) - (10 - 5) - (10 - 1))
        storage(2).asInstanceOf[TestableFluidStack].fluidId shouldBe 1
        storage(3).amount shouldBe 10
        storage(4) shouldBe 'Empty
        storage(5).amount shouldBe 1
        storage(6).amount shouldBe 3
      }
      "overflow to the multiple empty tanks" in new withStorage {
        capacityMapper = _ => 10
        val toFill = new TestableFluidStack(1, 50)
        val ret    = storage.fill(toFill, true)
        ret shouldBe toFill.amount
        toFill.amount shouldBe 50
        storage(0).amount shouldBe 10
        storage(1).amount shouldBe 10
        storage(2).amount shouldBe 10
        storage(2).asInstanceOf[TestableFluidStack].fluidId shouldBe 1
        storage(3).amount shouldBe 10
        storage(4).amount shouldBe 10
        storage(4).asInstanceOf[TestableFluidStack].fluidId shouldBe 1
        storage(5).amount shouldBe 1
        storage(6).amount shouldBe 3
        storage(7).amount shouldBe 7
        storage(7).asInstanceOf[TestableFluidStack].fluidId shouldBe 1
      }
      "return only the amount filled" in new withStorage {
        capacityMapper = _ => 10
        val toFill = new TestableFluidStack(1, 500)
        val ret    = storage.fill(toFill, true)
        ret shouldBe 73
        toFill.amount shouldBe 500
        storage(0).amount shouldBe 10
        storage(1).amount shouldBe 10
        storage(2).amount shouldBe 10
        storage(2).asInstanceOf[TestableFluidStack].fluidId shouldBe 1
        storage(3).amount shouldBe 10
        storage(4).amount shouldBe 10
        storage(4).asInstanceOf[TestableFluidStack].fluidId shouldBe 1
        storage(5).amount shouldBe 1
        storage(6).amount shouldBe 3
        storage(7).amount shouldBe 10
        storage(7).asInstanceOf[TestableFluidStack].fluidId shouldBe 1
        storage(8).amount shouldBe 10
        storage(8).asInstanceOf[TestableFluidStack].fluidId shouldBe 1
        storage(9).amount shouldBe 10
        storage(9).asInstanceOf[TestableFluidStack].fluidId shouldBe 1
      }
    }
    "when draining" should {
      "with a FluidStack" should {
        "remove and set emptied stacks empty" in new withStorage {
          val toRemove = new TestableFluidStack(1, 1)
          val removed  = storage.drain(toRemove, true)
          removed.amount shouldBe 1
          toRemove.amount shouldBe 1
          removed.asInstanceOf[TestableFluidStack].fluidId shouldBe 1
          toRemove.amount shouldBe 1
          storage(0) shouldBe 'Empty
          storage(1).amount shouldBe 5
          storage(2) shouldBe 'Empty
        }
        "remove from the first tank found" in new withStorage {
          val toRemove = new TestableFluidStack(2, 2)
          val removed  = storage.drain(toRemove, true)
          removed.amount shouldBe 2
          toRemove.amount shouldBe 2
          removed.asInstanceOf[TestableFluidStack].fluidId shouldBe 2
          storage(0).amount shouldBe 1
          storage(1).amount shouldBe 5
          storage(3).amount shouldBe 1
          storage(5) shouldBe 'Empty
          storage(6).amount shouldBe 2
        }
        "return only that which was removed" in new withStorage {
          val toRemove = new TestableFluidStack(2, 200)
          val removed  = storage.drain(toRemove, true)
          removed.amount shouldBe 4
          toRemove.amount shouldBe 200
          removed.asInstanceOf[TestableFluidStack].fluidId shouldBe 2
          storage(0).amount shouldBe 1
          storage(1).amount shouldBe 5
          storage(3).amount shouldBe 1
          storage(5) shouldBe 'Empty
          storage(6) shouldBe 'Empty
        }
      }
      "with an amount" should {
        "return from the first fluidstack found" in new withStorage {
          val toRemove = 5
          val removed  = storage.drainIStack(toRemove, true)
          removed.amount shouldBe 5
          removed.asInstanceOf[TestableFluidStack].fluidId shouldBe 1
          storage(0) shouldBe 'Empty
          storage(1).amount shouldBe 1
          storage(3).amount shouldBe 1
        }
        "return only those fluids matching the first fluidstack found" in new withStorage {
          val toRemove = 30
          val removed  = storage.drainIStack(toRemove, true)
          removed.amount shouldBe 7
          removed.asInstanceOf[TestableFluidStack].fluidId shouldBe 1
          storage(0) shouldBe 'Empty
          storage(1) shouldBe 'Empty
          storage(3) shouldBe 'Empty
          storage(5).amount shouldBe 1
          storage(6).amount shouldBe 3
        }
      }
    }
  }
}
