package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.{Converter, IItemStack}
import net.minecraft.entity.player.InventoryPlayer

/**
  * Created by Chris on 12/17/2016.
  */
class ItemStoragePlayerArmorInventory(playerInv: InventoryPlayer) extends IItemStorage {
  /**
    *
    * @param i Index
    *
    * @return Get IItemStack contained at this location.  This should never return null, as IItemStacks track their own emptiness.
    */
  override def apply(i: Int): IItemStack = Converter.IItemStackFromItemStack(playerInv.armorInventory.get(i))

  /**
    *
    * @param i Index to update
    * @param s IItemStack to set
    */
  override def update(i: Int, s: IItemStack): Unit = {
    playerInv.armorInventory.set(i, s.toMinecraft)
    playerInv.markDirty()
  }

  override def length: Int = playerInv.armorInventory.size()
}
