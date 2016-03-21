package com.itszuvalex.itszulib.gui

import net.minecraft.inventory.{IInventory, Slot}
import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 2/27/2016.
  */
class FilteredSlot(inventory: IInventory, slot: Int, x: Int, y: Int) extends Slot(inventory, slot, x, y) {
  override def isItemValid(item: ItemStack): Boolean = inventory.isItemValidForSlot(slot, item)
}
