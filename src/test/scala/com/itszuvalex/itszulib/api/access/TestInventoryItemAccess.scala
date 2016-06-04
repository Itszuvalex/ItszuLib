package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.access.AccessHelpers.InventoryArrayAdapter
import com.itszuvalex.itszulib.api.wrappers.WrappedItemStack
import com.itszuvalex.itszulib.testing.StubItem
import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/2016.
  */
class TestInventoryItemAccess extends TestBase {

  trait Collection {val collection: InventoryItemCollectionAccess}

  class EmptyCollection extends Collection with AccessHelpers.EmptyItemArray {override val collection = new InventoryItemCollectionAccess(new InventoryArrayAdapter(array))}

  class PartialCollection extends Collection with AccessHelpers.PartialItemArray {override val collection = new InventoryItemCollectionAccess(new InventoryArrayAdapter(array))}

  class Access(i: Int, collectionAccess: Collection) {
    val access     = new InventoryItemAccess(collectionAccess.collection, i)
    val collection = collectionAccess.collection
  }

  "A ArrayItemAccess" when {
    "referencing an empty Array slot" should {
      "return ItemStack None" in new Access(0, new EmptyCollection) {
        access.get shouldBe empty
      }
      "return CurrentStorage None" in new Access(0, new EmptyCollection) {
        access.currentStorage shouldBe empty
      }
      "return MaxStorage None" in new Access(0, new EmptyCollection) {
        access.maxStorage shouldBe empty
      }
      "return Damage None" in new Access(0, new EmptyCollection) {
        access.damage shouldBe empty
      }
      "return maxDamage None" in new Access(0, new EmptyCollection) {
        access.maxDamage shouldBe empty
      }
    }
    "referencing an occupied Array slot" should {
      "for getItemStack" must {
        "return ItemStack Some(_)" in new Access(0, new PartialCollection) {
          access.get should not be empty
        }
//        "return ItemStack from backing inventory" in new Access(0, new PartialCollection) {
//          access.get.get should be theSameInstanceAs collection.inventory.getStackInSlot(0)
//        }
      }
      "for currentStorage" must {
        "return CurrentStorage Some(_)" in new Access(0, new PartialCollection) {
          access.currentStorage should not be empty
        }
        "return StackSize of ItemStack" in new Access(0, new PartialCollection) {
          access.currentStorage.get shouldEqual access.get.get.stackSize
        }
      }
      "for maxStorage" must {
        "return MaxStorage Some(_)" in new Access(0, new PartialCollection) {
          access.maxStorage should not be empty
        }
        "return MaxStackSize of ItemStack" in new Access(0, new PartialCollection) {
          access.maxStorage.get shouldEqual access.get.get.stackSizeMax
        }
      }
      "for Damage" must {
        "return Damage Some(_)" in new Access(0, new PartialCollection) {
          access.damage should not be empty
        }
        "return damage value of ItemStack" in new Access(0, new PartialCollection) {
          access.damage.get shouldEqual access.get.get.damage
        }
      }
      "for MaxDamage" must {
        "return MaxDamage Some(_)" in new Access(0, new PartialCollection) {
          access.maxDamage should not be empty
        }
        "return MaxDamage value of ItemStack" in new Access(0, new PartialCollection) {
          access.maxDamage.get shouldEqual access.get.get.damageMax
        }
      }
      "increment" should {
        "return total increment when incrementing less than remaining space" in new Access(0, new PartialCollection) {
          access.increment(5) shouldBe 5
          access.currentStorage.get shouldBe 6
        }
        "return remaining space when incrementing more than remaining space" in new Access(7, new PartialCollection) {
          access.increment(5) shouldBe 1
          access.currentStorage.get shouldBe access.maxStorage.get
        }
      }
      "decrement" should {
        "return total decrement when decrementing less than currentStorage" in new Access(3, new PartialCollection) {
          access.decrement(5) shouldBe 5
          access.currentStorage.get shouldBe 5
        }
        "return currentStorage and clear ItemStack when decrementing more than currentStorage" in new Access(1, new PartialCollection) {
          access.decrement(5) shouldBe 2
          access.get shouldBe empty
          access.currentStorage shouldBe empty
        }
      }
    }
    "referencing an out of bounds Array slot" should {
      "throw ArrayIndexOutOfBoundsException" in new Access(-1, new EmptyCollection) {
        intercept[ArrayIndexOutOfBoundsException](access.get)
      }
    }
    "referencing any object" must {
      "be Valid" in new Access(0, new EmptyCollection) {
        access shouldBe 'Valid
      }
      "set ItemStack and get same ItemStack out" in new Access(0, new EmptyCollection) {
        val item = WrappedItemStack(new ItemStack(new StubItem))
        access.set(item)
        access.get shouldBe defined
//        val out = access.get.get
//        item should be theSameInstanceAs out
      }
    }
  }
}
