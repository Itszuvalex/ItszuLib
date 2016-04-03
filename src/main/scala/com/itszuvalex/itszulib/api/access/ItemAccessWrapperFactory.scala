package com.itszuvalex.itszulib.api.access

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.{ChatComponentText, IChatComponent}

/**
  * Created by Christopher Harris (Itszuvalex) on 3/12/2016.
  */
object ItemAccessWrapperFactory {

  def wrap(inventory: IInventory): IItemCollectionAccess = {
    inventory match {
      case a: InventoryWrapper => a
      case _ => new InventoryWrapper(new InventoryItemCollectionAccess(inventory))
    }
  }

  def wrap(access: IItemCollectionAccess): IInventory = {
    access match {
      case a: InventoryWrapper => a
      case _ => new InventoryWrapper(access)
    }
  }


  class InventoryWrapper(access: IItemCollectionAccess) extends IItemCollectionAccess with IInventory {
    override def canPlayerAccess(player: EntityPlayer): Boolean = access.canPlayerAccess(player)

    override def decrStackSize(slot: Int, amount: Int): ItemStack = access.apply(slot).split(amount).get.orNull

    override def setField(id: Int, value: Int): Unit = {}

    override def removeStackFromSlot(index: Int): ItemStack = {
      val ia = access(index)
      val item = ia.get.orNull
      ia.clear()
      item
    }

    override def getField(id: Int): Int = 0

    override def getFieldCount: Int = 0

    override def openInventory(player: EntityPlayer): Unit = {}

    override def clear(): Unit = access.foreach(_.clear())

    override def closeInventory(player: EntityPlayer): Unit = {}

    override def getSizeInventory: Int = length

    override def length: Int = access.length

    override def getInventoryStackLimit: Int = 64

    override def markDirty(): Unit = access.onChanged(-1)

    override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = access.apply(slot).canSetTo(stack)

    override def getStackInSlot(slot: Int): ItemStack = access.apply(slot).get.orNull

    override def setInventorySlotContents(slot: Int, stack: ItemStack): Unit = access(slot).set(stack)

    override def isUseableByPlayer(player: EntityPlayer): Boolean = access.canPlayerAccess(player)

    override def getDisplayName: IChatComponent = new ChatComponentText("InventoryWrapper")

    override def getName: String = "InventoryWrapper"

    override def hasCustomName: Boolean = false

    override def apply(idx: Int): IItemAccess = access.apply(idx)
  }

}
