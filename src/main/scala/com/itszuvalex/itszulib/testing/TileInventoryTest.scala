package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.storage.{IItemStorage, ItemStorageArray}
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

  override def defaultStorage: IItemStorage = new ItemStorageArray(3)

  override def func_191420_l(): Boolean = false

  override def getFieldCount: Int = inventory.getFieldCount

  override def getField(id: Int): Int = inventory.getField(id)

  override def setField(id: Int, value: Int): Unit = inventory.setField(id, value)
}
