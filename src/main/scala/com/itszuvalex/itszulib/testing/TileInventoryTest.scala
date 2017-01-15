package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.Capabilities
import com.itszuvalex.itszulib.api.storage.{IItemStorage, ItemStorageArray}
import com.itszuvalex.itszulib.api.wrappers.IItemStack
import com.itszuvalex.itszulib.core.TileEntityBase
import com.itszuvalex.itszulib.core.traits.tile.TileInventory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing

/**
  * Created by Alex on 02.01.2017.
  */
class TileInventoryTest extends TileEntityBase with TileInventory {
  override def getMod: AnyRef = ItszuLib

  override def onSideActivate(player: EntityPlayer, side: EnumFacing): Boolean = {
    player.openGui(getMod, 1, getWorld, getPos.getX, getPos.getY, getPos.getZ)
    true
  }

  override def defaultStorage: IItemStorage = new ItemStorageArray(3) {
    /**
      * Does not actually prevent inserting.  This should be overridden by inventories that want to prevent insertion by players.
      * Insertion by machines should also check this, but in cases of output slots we can't just prevent insertion of items at the storage level.
      *
      * @param i     Index
      * @param stack Stack to insert
      *
      * @return True if this stack can be inserted.
      */
    override def canInsert(i: Int, stack: IItemStack): Boolean = {
      if (i == 2) stack.hasCapability(Capabilities.ITEM_BURNABLE, null)
      else true
    }
  }

  override def func_191420_l(): Boolean = false

  override def getFieldCount: Int = inventory.getFieldCount

  override def getField(id: Int): Int = inventory.getField(id)

  override def setField(id: Int, value: Int): Unit = inventory.setField(id, value)
}
