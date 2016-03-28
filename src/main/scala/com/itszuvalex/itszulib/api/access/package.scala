package com.itszuvalex.itszulib.api

import com.itszuvalex.itszulib.api.storage.ArrayItemCollectionStorage
import net.minecraft.entity.player.EntityPlayer

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/16.
  */
package object access {

  implicit class PlayerAccessImplicits(player: EntityPlayer) {

    def inventoryAccess: IItemCollectionAccess = new ArrayItemCollectionStorage(player.inventory.mainInventory) {
      override def onChanged(idx: Int): Unit = player.inventory.markDirty()
    }.getFullAccess

    def armorAccess: IItemCollectionAccess = new ArrayItemCollectionStorage(player.inventory.armorInventory) {
      override def onChanged(idx: Int): Unit = player.inventory.markDirty()
    }.getFullAccess

  }

}
