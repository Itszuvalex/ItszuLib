package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.wrappers.{IItemStack, WrappedItemStack}
import net.minecraft.inventory.IInventory

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class InventoryItemAccess(private[access] val inventoryAccess: InventoryItemCollectionAccess, private[access] val index: Int) extends IItemAccess {
  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  override def get: Option[IItemStack] = if (isValid) {
    Option(inventory.getStackInSlot(index)) match {
      case Some(a) => Option(WrappedItemStack(a))
      case None => None
    }
  } else None

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
  override def set(stack: IItemStack): Unit = {
    inventory.setInventorySlotContents(index, stack.toMinecraft.orNull)
    super.set(stack)
  }

  /**
    *
    * @param amount Amount to remove from this storage and transfer to a new one.
    * @return New item access
    */
  override def split(amount: Int): IItemAccess = new FloatingItemAccess(WrappedItemStack(inventory.decrStackSize(index, amount)))

  private[access] def inventory: IInventory = inventoryAccess.inventory
}
