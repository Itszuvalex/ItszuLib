package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.storage.IItemStorage
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.{ChatComponentText, IChatComponent}

/**
  * Created by Chris on 7/31/2016.
  */
class WrapperIItemStorage(storage: IItemStorage) extends IInventory {

  override def closeInventory(player: EntityPlayer): Unit = {}

  override def clear(): Unit = storage.indices.foreach(storage.update(_, IItemStack.Empty))

  override def openInventory(player: EntityPlayer): Unit = {}

  override def removeStackFromSlot(index: Int): ItemStack = {
    val ret = storage(index)
    storage.setSlot(index, IItemStack.Empty)
    ret.toMinecraft
  }

  override def getDisplayName: IChatComponent = new ChatComponentText("Inventory")

  override def getName: String = "storage"

  override def hasCustomName: Boolean = false

  override def decrStackSize(slot: Int, amount: Int) = storage.split(slot, amount).toMinecraft

  override def getSizeInventory = storage.length

  override def getInventoryStackLimit = 64

  override def isItemValidForSlot(slot: Int, item: ItemStack) = true

  override def setInventorySlotContents(slot: Int, item: ItemStack) = {
    storage(slot) = WrapperVanillaItemStack(item)
    markDirty()
  }

  override def isUseableByPlayer(player: EntityPlayer) = true

  override def getStackInSlot(slot: Int) = storage(slot).toMinecraft

  override def markDirty(): Unit = {}

  override def getFieldCount: Int = 0

  override def getField(id: Int): Int = 0

  override def setField(id: Int, value: Int): Unit = {}
}
