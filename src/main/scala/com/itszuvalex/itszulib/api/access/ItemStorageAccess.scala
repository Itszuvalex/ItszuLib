package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.storage.IItemStorage
import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
class ItemStorageAccess(private val storage: IItemStorage, private val index: Int) extends IItemAccess {
  private val revision = storage.getRevision

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  override def get: Option[ItemStack] = storage.getItemStack(index)

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  override def set(stack: ItemStack): Unit = {
    storage.setItemStack(index, stack)
    super.set(stack)
  }

  /**
    *
    * @param amount Amount to remove from this storage and transfer to a new one.
    * @return New item access
    */
  override def split(amount: Int): IItemAccess = ???

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = revision == storage.getRevision
}
