package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.{IItemStack, WrapperVanillaItemStack}
import net.minecraft.inventory.IInventory

/**
  * Created by Christopher Harris (Itszuvalex) on 7/25/16.
  */
class ItemStorageInventory(val inv: IInventory) extends IItemStorage {
  /**
    *
    * @param i Index
    * @return Get IItemStack contained at this location.  This should never return null, as IItemStacks track their own emptiness.
    */
  override def apply(i: Int): IItemStack = WrapperVanillaItemStack(inv.getStackInSlot(i))

  /**
    *
    * @param i Index to update
    * @param s IItemStack to set
    */
  override def update(i: Int, s: IItemStack): Unit = inv.setInventorySlotContents(i, s.toMinecraft)

  override def length: Int = inv.getSizeInventory
}
