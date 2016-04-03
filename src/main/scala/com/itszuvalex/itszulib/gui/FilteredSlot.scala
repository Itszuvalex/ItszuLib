package com.itszuvalex.itszulib.gui

import net.minecraft.inventory.{IInventory, Slot}
import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 2/27/2016.
  */
class FilteredSlot(in_inventory: IInventory, slot: Int, x: Int, y: Int) extends Slot(in_inventory, slot, x, y) {
  override def isItemValid(item: ItemStack): Boolean = in_inventory.isItemValidForSlot(slot, item)
}
