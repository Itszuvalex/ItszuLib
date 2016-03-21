package com.itszuvalex.itszulib.api.access

import net.minecraft.inventory.IInventory

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class InventoryPartialRandomItemCollectionAccess(inventory: IInventory, private[access] val whitelist: Array[Int]) extends InventoryItemCollectionAccess(inventory) {

  override def length: Int = whitelist.length

  override def apply(idx: Int): IItemAccess = new InventoryItemAccess(this, whitelist(idx))
}
