package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.{IItemStack, WrapperVanillaItemStack}
import net.minecraftforge.items.IItemHandler

/**
  * Created by Chris on 7/31/2016.
  */
class ItemStorageItemHandler(handler: IItemHandler) extends IItemStorage {
  /**
    *
    * @param i Index
    * @return Get IItemStack contained at this location.  This should never return null, as IItemStacks track their own emptiness.
    */
  override def apply(i: Int): IItemStack = WrapperVanillaItemStack(handler.getStackInSlot(i))

  /**
    * K
    *
    * @param i Index to update
    * @param s IItemStack to set
    */
  override def update(i: Int, s: IItemStack): Unit = {
    handler.extractItem(i, handler.getStackInSlot(i).stackSize, false)
    handler.insertItem(i, s.toMinecraft, false)
  }

  override def length: Int = handler.getSlots
}
