package com.itszuvalex.itszulib.api.access

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class InventoryItemCollectionAccess(private[access] var inventory: IInventory) extends IItemCollectionAccess {
  override def canPlayerAccess(player: EntityPlayer): Boolean = inventory.isUseableByPlayer(player)

  override def length: Int = inventory.getSizeInventory

  override def apply(idx: Int): IItemAccess = new InventoryItemAccess(this, idx)

  private[itszuvalex] def updateBackingStore(inv: IInventory) = {
    inventory = inv
    onInventoryChanged(-1)
  }
}
