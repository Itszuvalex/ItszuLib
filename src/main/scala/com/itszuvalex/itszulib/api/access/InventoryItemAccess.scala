package com.itszuvalex.itszulib.api.access

import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class InventoryItemAccess(private[access] val inventoryAccess: InventoryItemCollectionAccess, private[access] val index: Int) extends IItemAccess {
  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  override def getItemStack: Option[ItemStack] = if (isValid) Option(inventory.getStackInSlot(index)) else None

  private[access] def inventory: IInventory = inventoryAccess.inventory

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = true //Always true because we don't cache, IInventory does.

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  override def setItemStack(stack: ItemStack): Unit = inventory.setInventorySlotContents(index, stack)

  /**
    *
    * @param amount Amount to remove from this storage and transfer to a new one.
    * @return New item access
    */
  override def split(amount: Int): IItemAccess = new FloatingItemAccess(inventory.decrStackSize(index, amount))
}
