package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.storage.IItemCollectionStorage
import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
class StorageItemAccess(private val storage: IItemCollectionStorage, private val index: Int) extends IItemAccess {
  private val revision = storage.getRevision

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  override def get: Option[ItemStack] = if (isValid) Option(storage(index)) else None

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = revision == storage.getRevision

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  override def set(stack: ItemStack): Unit = {
    storage(index) = stack
    onChanged()
  }

  /**
    * Call when this changes backing item.
    */
  override def onChanged(): Unit = storage.onChanged(index)
}
