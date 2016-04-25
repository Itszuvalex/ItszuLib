package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.api.core.Saveable
import com.itszuvalex.itszulib.api.storage.IItemCollectionStorage
import com.itszuvalex.itszulib.core.TileEntityBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.{ChatComponentText, EnumFacing, IChatComponent}

/**
  * Created by Chris on 11/29/2014.
  */
trait TileInventory extends TileEntityBase with ISidedInventory {
  @Saveable
  val inventory = defaultStorage

  def defaultStorage: IItemCollectionStorage


  override def getSlotsForFace(side: EnumFacing): Array[Int] = inventory.indices.toArray

  override def canExtractItem(index: Int, stack: ItemStack, direction: EnumFacing): Boolean = true

  override def canInsertItem(index: Int, itemStackIn: ItemStack, direction: EnumFacing): Boolean = true

  override def hasDescription: Boolean = false

  override def closeInventory(player: EntityPlayer): Unit = {}

  override def clear(): Unit = inventory.getFullAccess.foreach(_.clear())

  override def openInventory(player: EntityPlayer): Unit = {}

  override def removeStackFromSlot(index: Int): ItemStack = {
    val i = inventory.getFullAccess(index)
    val ret = i.get.orNull
    i.clear()
    ret
  }

  override def getDisplayName: IChatComponent = new ChatComponentText("Inventory")

  override def getName: String = "inventory"

  override def hasCustomName: Boolean = false

  override def decrStackSize(slot: Int, amount: Int) = inventory.getInventory.decrStackSize(slot, amount)

  override def getSizeInventory = inventory.getFullAccess.length

  override def getInventoryStackLimit = inventory.getInventory.getInventoryStackLimit

  override def isItemValidForSlot(slot: Int, item: ItemStack) = inventory.getInventory.isItemValidForSlot(slot, item)

  override def setInventorySlotContents(slot: Int, item: ItemStack) = {
    inventory.getInventory.setInventorySlotContents(slot, item)
    markDirty()
  }

  override def markDirty() = {
    setModified()
    notifyNeighborsOfChange()
  }

  override def isUseableByPlayer(player: EntityPlayer) = canPlayerUse(player) && inventory.getFullAccess.canPlayerAccess(player)

  override def getStackInSlot(slot: Int) = inventory.getInventory.getStackInSlot(slot)

}
