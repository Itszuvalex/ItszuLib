package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.container.ContainerInv
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.item.ItemStack

/**
  * Created by Alex on 12.10.2015.
  */
class ContainerTankTest(player: EntityPlayer, inv: InventoryPlayer, tile: TileTankTest) extends ContainerInv[TileTankTest](player, tile, 0, 0) {

  addPlayerInventorySlots(inv)

  override def eligibleForInput(item: ItemStack): Boolean = false
}
