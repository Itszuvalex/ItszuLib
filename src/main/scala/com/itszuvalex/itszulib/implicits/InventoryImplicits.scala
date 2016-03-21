package com.itszuvalex.itszulib.implicits

import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

/**
  * Created by Christopher on 8/21/2015.
  */
object InventoryImplicits {

  implicit class Inventory(list: IInventory) extends Iterable[ItemStack] {
    override def iterator: Iterator[ItemStack] = new InventoryIterator(this.list)

    class InventoryIterator(private val inventory: IInventory) extends Iterator[ItemStack] {
      var index = 0

      override def hasNext = index < inventory.getSizeInventory

      override def next(): ItemStack = {
        val ret = inventory.getStackInSlot(index)
        index += 1
        ret
      }
    }

  }

}
