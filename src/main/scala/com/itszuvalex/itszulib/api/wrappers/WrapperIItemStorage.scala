package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.storage.IItemStorage
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.text.ITextComponent

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

  override def isEmpty: Boolean = storage.forall(_.isEmpty)

  override def getDisplayName: ITextComponent = null

  override def getName: String = "storage"

  override def hasCustomName: Boolean = false

  override def decrStackSize(slot: Int, amount: Int): ItemStack = storage.split(slot, amount).toMinecraft

  override def getSizeInventory: Int = storage.length

  override def getInventoryStackLimit = 64

  override def isItemValidForSlot(slot: Int, item: ItemStack) = true

  override def setInventorySlotContents(slot: Int, item: ItemStack): Unit = {
    storage(slot) = WrapperVanillaItemStack(item)
    markDirty()
  }

  override def isUsableByPlayer(player: EntityPlayer) = true

  override def getStackInSlot(slot: Int): ItemStack = storage(slot).toMinecraft

  override def markDirty(): Unit = {}

  override def getFieldCount: Int = 0

  override def getField(id: Int): Int = 0

  override def setField(id: Int, value: Int): Unit = {}
}
