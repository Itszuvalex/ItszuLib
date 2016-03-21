package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.api.core.Saveable
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.core.TileEntityBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack

/**
  * Created by Chris on 11/29/2014.
  */
trait TileInventory extends TileEntityBase with ISidedInventory {
  @Saveable
  val inventory = defaultStorage

  def defaultStorage: IItemStorage

  override def getAccessibleSlotsFromSide(side: Int) = inventory.getAccess.indices.toArray

  override def canExtractItem(slot: Int, item: ItemStack, side: Int) = true

  override def canInsertItem(slot: Int, item: ItemStack, side: Int) = true

  override def closeInventory() = {}

  override def decrStackSize(slot: Int, amount: Int) = inventory.getInventory.decrStackSize(slot, amount)

  override def getSizeInventory = inventory.getAccess.length

  override def getInventoryStackLimit = inventory.getInventory.getInventoryStackLimit

  override def isItemValidForSlot(slot: Int, item: ItemStack) = inventory.getInventory.isItemValidForSlot(slot, item)

  override def getStackInSlotOnClosing(slot: Int): ItemStack = inventory.getInventory.getStackInSlotOnClosing(slot)

  override def openInventory() = {}

  override def setInventorySlotContents(slot: Int, item: ItemStack) = {
    inventory.getInventory.setInventorySlotContents(slot, item)
    markDirty()
  }

  override def markDirty() = {
    setModified()
    notifyNeighborsOfChange()
  }

  override def isUseableByPlayer(player: EntityPlayer) = canPlayerUse(player) && inventory.getAccess.canPlayerAccess(player)

  override def getStackInSlot(slot: Int) = inventory.getInventory.getStackInSlot(slot)

  override def hasCustomInventoryName = false

  override def getInventoryName = ""
}
