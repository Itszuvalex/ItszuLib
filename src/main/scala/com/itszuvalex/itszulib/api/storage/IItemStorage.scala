package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access._
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/13/2016.
  */
trait IItemStorage extends IStorage[IItemStorage, IItemCollectionAccess, IItemAccess, ItemStack] {

  override def getFullAccess: IItemCollectionAccess = new StorageItemCollectionAccess(this)

  def getInventory: IInventory = ItemAccessWrapperFactory.wrap(getFullAccess)
}
