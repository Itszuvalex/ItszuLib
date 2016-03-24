package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.storage.IItemStorage
import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
class StorageItemAccess(private val storage: IItemStorage, private val index: Int) extends IItemAccess {
  private val revision = storage.getRevision

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  override def getItemStack: Option[ItemStack] = storage.getItemStack(index)

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  override def setItemStack(stack: ItemStack): Unit = {
    storage.setItemStack(index, stack)
    onChanged()
  }

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = revision == storage.getRevision
}
