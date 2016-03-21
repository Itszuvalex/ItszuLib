package com.itszuvalex.itszulib.api.access

import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class FloatingItemAccess(item: ItemStack) extends IItemAccess {
  private[access] var backingItem = Option(item)

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = true

  /**
    *
    * @param amount Amount to remove from this storage and transfer to a new one.
    * @return New item access
    */
  override def split(amount: Int): IItemAccess = amount match {
    case invalid if invalid <= 0 => new FloatingItemAccess(null)
    case other => new FloatingItemAccess(
                                          getItemStack.map { i =>
                                            val item = i.copy()
                                            val size = Math.min(currentStorage.get, amount)
                                            item.stackSize = size
                                            decrement(size)
                                            item
                                                           }.orNull
                                        )
  }

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  override def getItemStack: Option[ItemStack] = backingItem

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  override def setItemStack(stack: ItemStack): Unit = backingItem = Option(stack)
}
