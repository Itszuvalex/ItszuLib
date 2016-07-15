package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.api.core.Saveable
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.{IItemStack, WrapperVanillaItemStack}
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

  def defaultStorage: IItemStorage

  override def getSlotsForFace(side: EnumFacing): Array[Int] = inventory.indices.toArray

  override def canExtractItem(index: Int, stack: ItemStack, direction: EnumFacing): Boolean = true

  override def canInsertItem(index: Int, itemStackIn: ItemStack, direction: EnumFacing): Boolean = true

  override def hasDescription: Boolean = false

  override def closeInventory(player: EntityPlayer): Unit = {}

  override def clear(): Unit = inventory.indices.foreach(inventory.update(_, IItemStack.Empty))

  override def openInventory(player: EntityPlayer): Unit = {}

  override def removeStackFromSlot(index: Int): ItemStack = {
    val ret = inventory(index)
    inventory.setSlot(index, IItemStack.Empty)
    ret.toMinecraft
  }

  override def getDisplayName: IChatComponent = new ChatComponentText("Inventory")

  override def getName: String = "inventory"

  override def hasCustomName: Boolean = false

  override def decrStackSize(slot: Int, amount: Int) = inventory.split(slot, amount).toMinecraft

  override def getSizeInventory = inventory.length

  override def getInventoryStackLimit = 64

  override def isItemValidForSlot(slot: Int, item: ItemStack) = true

  override def setInventorySlotContents(slot: Int, item: ItemStack) = {
    inventory(slot) = WrapperVanillaItemStack(item)
    markDirty()
  }

  override def markDirty() = {
    setModified()
    notifyNeighborsOfChange()
  }

  override def isUseableByPlayer(player: EntityPlayer) = canPlayerUse(player)

  override def getStackInSlot(slot: Int) = inventory(slot).toMinecraft

}
