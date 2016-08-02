package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.{IItemStack, WrapperVanillaItemStack}
import net.minecraft.inventory.ISidedInventory
import net.minecraft.util.EnumFacing

/**
  * Created by Christopher Harris (Itszuvalex) on 7/25/16.
  */
class ItemStorageSidedInventory(val inv: ISidedInventory, side: EnumFacing) extends IItemStorage {
  val slots = inv.getSlotsForFace(side)

  /**
    *
    * @param i Index
    * @return Get IItemStack contained at this location.  This should never return null, as IItemStacks track their own emptiness.
    */
  override def apply(i: Int): IItemStack = WrapperVanillaItemStack(inv.getStackInSlot(slots(i)))

  /**
    *
    * @param i Index to update
    * @param s IItemStack to set
    */
  override def update(i: Int, s: IItemStack): Unit = inv.setInventorySlotContents(slots(i), s.toMinecraft)

  override def length: Int = slots.length
}
