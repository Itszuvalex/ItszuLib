package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.testing.{StubFluid, StubItem}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/2016.
  */
object AccessHelpers extends TestBase {

  val testNBTItemDeserializer = (nbt: NBTTagCompound) => {
    if (nbt == null)
      null
    else if (nbt.hasNoTags)
      null
    else {
      val item = new ItemStack(new StubItem())
      item.stackSize = nbt.getByte("Count")
      item.setItemDamage(nbt.getShort("Damage"))

      if (item.getItemDamage < 0) {
        item.setItemDamage(0)
      }
      item
    }
  }

  val testNBTFluidDeserializer = (nbt: NBTTagCompound) => {
    if (nbt == null)
      null
    else if (nbt.hasNoTags)
      null
    else {
      val amt = nbt.getInteger("Amount")
      var tag: NBTTagCompound = null
      if (nbt.hasKey("NBT")) {
        tag = nbt.getCompoundTag("NBT")
      }
      val fluid = mockFluid(amt, tag)
      fluid
    }
  }

  val testNBTFluidSerializer = (f: FluidStack, nbt: NBTTagCompound) => {
    if (f == null || nbt == null)
      ()
    else {
      nbt.setInteger("Amount", f.amount)
      if (f.tag != null && !f.tag.hasNoTags) {
        nbt.setTag("NBT", f.tag)
      }
    }
  }


  class DefaultFluid extends FluidStack(new StubFluid(), 0)

//  def mockFluid(amt: Int, tag: NBTTagCompound = null): FluidStack = {
//    val stubStack = stub[DefaultFluid]
//    stubStack.amount = amt
//    stubStack.tag = tag
//    stubStack
//  }

  trait EmptyItemArray {
    val array = new Array[ItemStack](10)
  }

  trait PartialItemArray {
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

//  trait EmptyFluidArray {
//    val array = new Array[FluidStack](10)
//  }
//
//  trait PartialFluidArray {
//    val array  = new Array[FluidStack](10)
//    val fluid0 = mockFluid(0)
//    val fluid1 = mockFluid(200)
//    val fluid3 = mockFluid(1000)
//    val fluid7 = mockFluid(2000)
//    array(0) = fluid0
//    array(1) = fluid1
//    array(3) = fluid3
//    array(7) = fluid7
//  }


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
