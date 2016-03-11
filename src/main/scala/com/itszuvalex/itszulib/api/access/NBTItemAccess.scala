package com.itszuvalex.itszulib.api.access

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.collection.JavaConversions._

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class NBTItemAccess(private[access] val nbtAccess: NBTItemCollectionAccess, private[access] val index: Int) extends IItemAccess {
  private[access] val revision                = nbtAccess.getRevision
  private[access] val nbt                     = nbtAccess.nbt
  private[access] var item: Option[ItemStack] = getItemCompound(false) match {
    case None => None
    case Some(comp) => Some(ItemStack.loadItemStackFromNBT(comp))
  }

  /**
    *
    * @param amount Amount to remove from this storage and transfer to a new one.
    * @return New item access
    */
  override def split(amount: Int): IItemAccess = amount match {
    case invalid if invalid <= 0 => new FloatingItemAccess(null)
    case other => new FloatingItemAccess(
                                          getItemStack.map { i =>
                                            val item = i.copy()
                                            val size = Math.min(currentStorage.get, amount)
                                            item.stackSize = size
                                            decrement(size)
                                            item
                                                           }.orNull
                                        )
  }

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  override def getItemStack: Option[ItemStack] = if (isValid) item else None

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = revision == nbtAccess.getRevision

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  override def setItemStack(stack: ItemStack): Unit = {
    if (isValid) {
      item = Option(stack)
      onItemChanged()
    }
  }

  /**
    * Call when backing item changes.
    */
  override def onItemChanged(): Unit = {
    super.onItemChanged()
    (getItemStack, getItemCompound(force = getItemStack.isDefined)) match {
      case (None, None) =>
      case (None, Some(_)) => nbt.removeTag(index.toString)
      case (Some(i), Some(comp)) =>
        comp.func_150296_c().collect { case s: String => s }.foreach(comp.removeTag)
        i.writeToNBT(comp)
    }
    nbtAccess.onInventoryChanged(index)
  }

  def getItemCompound(force: Boolean = false): Option[NBTTagCompound] = {
    val exists = nbt.hasKey(index.toString)
    if (exists || force) {
      if (force && !exists) {
        nbt.setTag(index.toString, new NBTTagCompound)
      }
      Option(nbt.getCompoundTag(index.toString))
    }
    else None
  }
}