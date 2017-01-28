package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.container.ContainerInv
import com.itszuvalex.itszulib.container.sync.SyncItemStorageItemStack
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.item.ItemStack

/**
  * Created by Alex on 02.01.2017.
  */
class ContainerInventoryTest(player: EntityPlayer, inv: InventoryPlayer, tile: TileInventoryTest, registerSyncs: Boolean) extends ContainerInv[TileInventoryTest](player, tile, 0, 0, 1, registerSyncs) {

  if (registerSyncs) {
    addSync(new SyncItemStorageItemStack(GuiID, tile.storage, 0))
    addSync(new SyncItemStorageItemStack(GuiID, tile.storage, 1))
    addSync(new SyncItemStorageItemStack(GuiID, tile.storage, 2))
    addPlayerInventorySlots(inv)
  }

  override def eligibleForInput(item: ItemStack): Boolean = true
}
