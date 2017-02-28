package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.api.Capabilities
import com.itszuvalex.itszulib.api.core.Saveable
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.Converter
import com.itszuvalex.itszulib.core.TileEntityBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.text.ITextComponent
import net.minecraftforge.common.capabilities.Capability

/**
  * Created by Chris on 11/29/2014.
  */
trait TileInventory extends TileEntityBase with ISidedInventory {
  @Saveable
  val storage   = defaultStorage
  val inventory = Converter.IInventoryFromIItemStorage(storage)

  def defaultStorage: IItemStorage

  override def getSlotsForFace(side: EnumFacing): Array[Int] = storage.indices.toArray

  override def canExtractItem(index: Int, stack: ItemStack, direction: EnumFacing): Boolean = true

  override def canInsertItem(index: Int, itemStackIn: ItemStack, direction: EnumFacing): Boolean = true

  override def hasDescription: Boolean = false

  override def closeInventory(player: EntityPlayer): Unit = inventory.closeInventory(player)

  override def clear(): Unit = inventory.clear()

  override def openInventory(player: EntityPlayer): Unit = inventory.openInventory(player)

  override def removeStackFromSlot(index: Int): ItemStack = inventory.removeStackFromSlot(index)

  override def getDisplayName: ITextComponent = inventory.getDisplayName

  override def getName: String = inventory.getName

  override def isEmpty: Boolean = inventory.isEmpty

  override def hasCustomName: Boolean = inventory.hasCustomName

  override def decrStackSize(slot: Int, amount: Int) = inventory.decrStackSize(slot, amount)

  override def getSizeInventory = inventory.getSizeInventory

  override def getInventoryStackLimit = inventory.getInventoryStackLimit

  override def isItemValidForSlot(slot: Int, item: ItemStack) = inventory.isItemValidForSlot(slot, item)

  override def setInventorySlotContents(slot: Int, item: ItemStack) = {
    inventory.setInventorySlotContents(slot, item)
    markDirty()
  }

  override def markDirty() = {
    setModified()
    notifyNeighborsOfChange()
  }

  override def isUsableByPlayer(player: EntityPlayer): Boolean = canPlayerUse(player)

  override def getStackInSlot(slot: Int) = inventory.getStackInSlot(slot)

  override def getCapability[T](capability: net.minecraftforge.common.capabilities.Capability[T], facing: net.minecraft.util.EnumFacing): T = {
    if (capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      Converter.IItemHandlerModifiableFromIItemStorage(storage).asInstanceOf[T]
    }
    else if (capability == Capabilities.ITEM_STORAGE) {
      storage.asInstanceOf[T]
    }
    else
      super.getCapability[T](capability, facing)
  }

  override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
    if (capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) true
    else if (capability == Capabilities.ITEM_STORAGE) true
    else
      super.hasCapability(capability, facing)
  }

}
