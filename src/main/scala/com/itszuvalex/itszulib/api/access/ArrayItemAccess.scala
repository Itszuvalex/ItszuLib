package com.itszuvalex.itszulib.api.access

import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class ArrayItemAccess(private[access] val arrayAccess: ArrayItemCollectionAccess, private[access] val index: Int) extends IItemAccess {
  private[access] def array = arrayAccess.array

  /**
    *
    * @param amount Amount to remove from this storage and transfer to a new one.
    * @return New item access
    */
  override def split(amount: Int): IItemAccess = amount match {
    case invalid if invalid <= 0 => new FloatingItemAccess(null)
    case _ => new FloatingItemAccess(
                                      getItemStack.map { i =>
                                        val item = i.copy()
                                        item.stackSize = decrement(amount)
                                        if (item.stackSize > 0)
                                          item
                                        else null
                                                       }.orNull
                                    )
  }

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  override def getItemStack: Option[ItemStack] = Option(array(index))

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  override def setItemStack(stack: ItemStack): Unit = {
    array(index) = stack
    onChanged()
  }

  /**
    * Call when backing item changes.
    */
  override def onChanged(): Unit = {
    super.onChanged()
    arrayAccess.onInventoryChanged(index)
  }

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = true //Always true because we don't cache - the backing array does.
}
