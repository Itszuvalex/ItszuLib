package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.testing.StubItem
import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/2016.
  */
class TestIItemAccess extends TestBase {

  trait Access {val access: StubIItemAccess}

  trait EmptyAccess extends Access {override val access = new StubIItemAccess(null)}

  trait OtherAccess {val other = new StubIItemAccess(new ItemStack(new StubItem, 3, 5))}

  class DefinedAccess(i: Int = 1) extends Access {override val access = new StubIItemAccess(new ItemStack(new StubItem, i))}

  class StubIItemAccess(var itemStack: ItemStack) extends IItemAccess {
    var changed = false


    /**
      * Call when backing item changes.
      */
    override def onChanged(): Unit = {
      changed = true
    }

    /**
      * Don't use unless absolutely necessary
      *
      * @return Backing ItemStack
      */
    override def getItemStack: Option[ItemStack] = Option(itemStack)

    /**
      * Sets this item access's storage to the ItemStack.
      *
      * @param stack ItemStack to set this to.
      */
    override def setItemStack(stack: ItemStack): Unit = itemStack = stack

    /**
      *
      * @param amount Amount to remove from this storage and transfer to a new one.
      * @return New item access
      */
    override def split(amount: Int): IItemAccess = null

    /**
      *
      * @return True if this access is still valid.  False if underlying storage is no longer correct.
      */
    override def isValid: Boolean = true
  }


  "An IItemAccess bare implementation" should {
    "when ItemStack == None" should {
      "currentStorage should be empty" in new EmptyAccess {
        access.currentStorage shouldBe empty
      }
      "maxStorage should be empty" in new EmptyAccess {
        access.maxStorage shouldBe empty
      }
      "damage should be empty" in new EmptyAccess {
        access.damage shouldBe empty
      }
      "maxDamage should be empty" in new EmptyAccess {
        access.maxDamage shouldBe empty
      }
      "decrement should return 0 and not call ItemChanged" in new EmptyAccess {
        access.decrement(0) shouldBe 0
        access should not be 'changed
      }
      "increment should return 0 and not call ItemChanged" in new EmptyAccess {
        access.increment(0) shouldBe 0
        access should not be 'changed
      }
      "when copying from an empty access should remain empty" in new EmptyAccess {
        val other = new StubIItemAccess(null)
        access.copyFromAccess(other)
        access.getItemStack shouldBe empty
      }
      "when copying from a defined access" should {
        "become defined when using ItemStack reference" in new EmptyAccess with OtherAccess {
          access.copyFromAccess(other, false)
          access.getItemStack should not be empty
          access.currentStorage.get shouldEqual other.currentStorage.get
          access.damage.get shouldEqual other.damage.get
        }
        "become defined when copying ItemStack" in new EmptyAccess with OtherAccess {
          access.copyFromAccess(other, true)
          access.getItemStack should not be empty
          access.currentStorage.get shouldEqual other.currentStorage.get
          access.damage.get shouldEqual other.damage.get
        }
      }
    }
    "when ItemStack == Some(_)" should {
      "currentStorage should be backing itemStack.stackSize" in new DefinedAccess {
        access.currentStorage should not be empty
        access.currentStorage.get shouldEqual access.itemStack.stackSize
      }
      "maxStorage should be backing itemStack.maxStackSize" in new DefinedAccess {
        access.currentStorage should not be empty
        access.maxStorage.get shouldEqual access.itemStack.getMaxStackSize
      }
      "damage should be itemStack.damage" in new DefinedAccess {
        access.damage should not be empty
        access.damage.get shouldEqual access.itemStack.getItemDamage
      }
      "maxDamage should be itemStack.damage" in new DefinedAccess {
        access.maxDamage should not be empty
        access.maxDamage.get shouldEqual access.itemStack.getMaxDamage
      }
      "increment" should {
        "return total increment when incrementing less than remaining space" in new DefinedAccess(1) {
          access.increment(5) shouldBe 5
          access.currentStorage.get shouldBe 6
          access shouldBe 'changed
        }
        "return remaining space when incrementing more than remaining space" in new DefinedAccess(63) {
          access.increment(5) shouldBe 1
          access.currentStorage.get shouldBe access.maxStorage.get
          access shouldBe 'changed
        }
      }
      "decrement" should {
        "return total decrement when decrementing less than currentStorage" in new DefinedAccess(10) {
          access.decrement(5) shouldBe 5
          access.currentStorage.get shouldBe 5
          access shouldBe 'changed
        }
        "return currentStorage and clear ItemStack when decrementing more than currentStorage" in new DefinedAccess(2) {
          access.decrement(5) shouldBe 2
          access.getItemStack shouldBe empty
          access.currentStorage shouldBe empty
          access shouldBe 'changed
        }
      }
      "when copying from an empty access should become empty" in new DefinedAccess(2) {
        val other = new StubIItemAccess(null)
        access.copyFromAccess(other, true)
        access.getItemStack shouldBe empty
      }
      "when copying from a defined access" should {
        "match other when taking ItemStack reference" in new DefinedAccess(2) with OtherAccess {
          access.copyFromAccess(other, false)
          access.currentStorage.get shouldEqual other.currentStorage.get
          access.damage.get shouldEqual other.damage.get
        }
        "match other when copying ItemStack" in new DefinedAccess(2) with OtherAccess {
          access.copyFromAccess(other, true)
          access.currentStorage.get shouldEqual other.currentStorage.get
          access.damage.get shouldEqual other.damage.get
        }
      }
    }
  }
}
