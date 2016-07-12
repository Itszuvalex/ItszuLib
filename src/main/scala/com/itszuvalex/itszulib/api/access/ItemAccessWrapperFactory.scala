package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.wrappers.WrappedItemStack
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.{ChatComponentText, IChatComponent}
import net.minecraftforge.items.IItemHandlerModifiable

/**
  * Created by Christopher Harris (Itszuvalex) on 3/12/2016.
  */
object ItemAccessWrapperFactory {

  def handlerToAccess(handler: IItemHandlerModifiable): IItemCollectionAccess = {
    handler match {
      case a: ItemHandlerWrapper => a
      case _ => new ItemHandlerWrapper(new ItemHandlerItemCollectionAccess(handler))
    }
  }

  def accesToHandler(access: IItemCollectionAccess): IItemHandlerModifiable = {
    access match {
      case a: ItemHandlerWrapper => a
      case _ => new ItemHandlerWrapper(access)
    }
  }

  def inventoryToAccess(inventory: IInventory): IItemCollectionAccess = {
    inventory match {
      case a: InventoryWrapper => a
      case _ => new InventoryWrapper(new InventoryItemCollectionAccess(inventory))
    }
  }

  def accessToInventory(access: IItemCollectionAccess): IInventory = {
    access match {
      case a: InventoryWrapper => a
      case _ => new InventoryWrapper(access)
    }
  }

  class ItemHandlerWrapper(private val access: IItemCollectionAccess) extends IItemCollectionAccess with IItemHandlerModifiable {
    override def extractItem(i: Int, i1: Int, b: Boolean): ItemStack = {
      val iaccess = access.apply(i)
      iaccess.get match {
        case None => null
        case Some(a) =>
          val ret = new FloatingItemAccess(a.copy)
          val slotCopy = if (b) new FloatingItemAccess(a.copy) else iaccess
          ret.get.get.stackSize = 0
          ret.increment(slotCopy.decrement(i1))
          ret.get.flatMap(_.toMinecraft).orNull
      }
    }

    override def getSlots: Int = length

    override def insertItem(i: Int, itemStack: ItemStack, b: Boolean): ItemStack = {
      val iaccess = access.apply(i)
      iaccess.get match {
        case None => itemStack
        case Some(a) =>
          val inc = new FloatingItemAccess(WrappedItemStack(Option(itemStack).map(_.copy()).orNull))
          val target = access(i)
          val source = if (b) new FloatingItemAccess(inc.get.get.copy) else inc
          val dest = if (b) new FloatingItemAccess(target.get.get.copy) else target
          source.transfer(dest, source.currentStorage.get)
          source.get.get.toMinecraft.orNull
      }
    }

    override def getStackInSlot(i: Int): ItemStack = access.apply(i).get.flatMap(_.toMinecraft).orNull

    override def canPlayerAccess(player: EntityPlayer): Boolean = access.canPlayerAccess(player)

    override def length: Int = access.length

    override def apply(idx: Int): IItemAccess = access(idx)

    override def setStackInSlot(i: Int, itemStack: ItemStack): Unit = access.apply(i).set(WrappedItemStack(itemStack))
  }

  class InventoryWrapper(private val access: IItemCollectionAccess) extends IItemCollectionAccess with IInventory {
    override def canPlayerAccess(player: EntityPlayer): Boolean = access.canPlayerAccess(player)

    override def decrStackSize(slot: Int, amount: Int): ItemStack = access.apply(slot).split(amount).get.flatMap(_.toMinecraft).orNull

    override def setField(id: Int, value: Int): Unit = {}

    override def removeStackFromSlot(index: Int): ItemStack = {
      val ia = access(index)
      val item = ia.get.orNull
      if (item == null)
        null
      else {
        ia.clear()
        item.toMinecraft.orNull
      }
    }

    override def getField(id: Int): Int = 0

    override def getFieldCount: Int = 0

    override def openInventory(player: EntityPlayer): Unit = {}

    override def clear(): Unit = access.foreach(_.clear())

    override def closeInventory(player: EntityPlayer): Unit = {}

    override def getSizeInventory: Int = length

    override def length: Int = access.length

    override def getInventoryStackLimit: Int = 64

    override def markDirty(): Unit = access.onChanged(-1)

    override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = access.apply(slot).canSetTo(WrappedItemStack(stack))

    override def getStackInSlot(slot: Int): ItemStack = access.apply(slot).get.flatMap(_.toMinecraft).orNull

    override def setInventorySlotContents(slot: Int, stack: ItemStack): Unit = access(slot).set(WrappedItemStack(stack))

    override def isUseableByPlayer(player: EntityPlayer): Boolean = access.canPlayerAccess(player)

    override def getDisplayName: IChatComponent = new ChatComponentText("InventoryWrapper")

    override def getName: String = "InventoryWrapper"

    override def hasCustomName: Boolean = false

    override def apply(idx: Int): IItemAccess = access.apply(idx)
  }

}
