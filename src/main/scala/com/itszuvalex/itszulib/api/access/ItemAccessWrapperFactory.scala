package com.itszuvalex.itszulib.api.access

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

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

    override def decrStackSize(slot: Int, amount: Int): ItemStack = access.apply(slot).split(amount).getItemStack.orNull

    override def closeInventory(): Unit = {}

    override def getSizeInventory: Int = length

    override def length: Int = access.length

    override def getInventoryStackLimit: Int = 64

    override def markDirty(): Unit = access.onInventoryChanged(-1)

    override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = access.apply(slot).isItemValid(stack)

    override def getStackInSlotOnClosing(slot: Int): ItemStack = getStackInSlot(slot)

    override def getStackInSlot(slot: Int): ItemStack = access.apply(slot).getItemStack.orNull

    override def openInventory(): Unit = {}

    override def setInventorySlotContents(slot: Int, stack: ItemStack): Unit = access(slot).setItemStack(stack)

    override def isUseableByPlayer(player: EntityPlayer): Boolean = access.canPlayerAccess(player)

    override def hasCustomInventoryName: Boolean = false

    override def getInventoryName: String = ""

    override def apply(idx: Int): IItemAccess = access.apply(idx)
  }

}
