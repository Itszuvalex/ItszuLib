package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.testing.StubItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/2016.
  */
object AccessHelpers extends TestBase {

  trait EmptyArray {
    val array = new Array[ItemStack](10)
  }

  trait PartialArray {
    val array = new Array[ItemStack](10)
    val item0 = new ItemStack(new StubItem)
    val item1 = new ItemStack(new StubItem, 2)
    val item3 = new ItemStack(new StubItem, 10)
    val item7 = new ItemStack(new StubItem, 63)
    array(0) = item0
    array(1) = item1
    array(3) = item3
    array(7) = item7
  }

  class InventoryArrayAdapter(array: Array[ItemStack]) extends IInventory {
    override def decrStackSize(slot: Int, amt: Int): ItemStack = {
      val item = array(slot)
      if (item == null) return null
      val copy = item.copy()
      val amount = Math.min(amt, item.stackSize)
      item.stackSize -= amount
      if (item.stackSize <= 0)
        setInventorySlotContents(slot, null)
      copy.stackSize = amount
      if (copy.stackSize > 0)
        copy
      else null
    }

    override def setInventorySlotContents(slot: Int, item: ItemStack): Unit = {
      array(slot) = item
      markDirty()
    }

    override def markDirty(): Unit = {}

    override def closeInventory(): Unit = {}

    override def getSizeInventory: Int = array.length

    override def getInventoryStackLimit: Int = 64

    override def isItemValidForSlot(slot: Int, item: ItemStack): Boolean = true

    override def getStackInSlotOnClosing(slot: Int): ItemStack = array(slot)

    override def openInventory(): Unit = {}

    override def isUseableByPlayer(player: EntityPlayer): Boolean = true

    override def getStackInSlot(slot: Int): ItemStack = array(slot)

    override def hasCustomInventoryName: Boolean = false

    override def getInventoryName: String = ""
  }

}
