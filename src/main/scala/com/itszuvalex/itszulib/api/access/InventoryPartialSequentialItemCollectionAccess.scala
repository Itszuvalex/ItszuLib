package com.itszuvalex.itszulib.api.access

import net.minecraft.inventory.IInventory

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class InventoryPartialSequentialItemCollectionAccess(inventory: IInventory, private[access] val indexStart: Int, private[access] val indexEnd: Int) extends InventoryItemCollectionAccess(inventory) {

  override def length: Int = indexEnd - indexStart + 1

  override def apply(idx: Int): IItemAccess = new InventoryItemAccess(this, idx + indexStart)
}
