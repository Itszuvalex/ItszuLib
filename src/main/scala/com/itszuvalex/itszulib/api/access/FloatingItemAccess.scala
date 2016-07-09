package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.wrappers.IItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class FloatingItemAccess(item: IItemStack) extends IItemAccess {
  private[access] var backingItem = Option(item)

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = true

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  override def get: Option[IItemStack] = backingItem

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  override def set(stack: IItemStack): Unit = {
    backingItem = Option(stack)
    super.set(stack)
  }
}
