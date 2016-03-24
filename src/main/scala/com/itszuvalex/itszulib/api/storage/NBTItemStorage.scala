package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.api.access.{IItemCollectionAccess, ItemAccessWrapperFactory, StorageItemCollectionAccess}
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.collection.JavaConversions._

/**
  * Created by Christopher Harris (Itszuvalex) on 3/23/16.
  */
object NBTItemStorage {
  val SIZE_KEY = "Size"

  val itemStackNBTDeserializer = new Overridable(ItemStack.loadItemStackFromNBT _)
  val itemStackNBTSerializer   = new Overridable((i: ItemStack, n: NBTTagCompound) => {
    i.writeToNBT(n)
    () // Fix Unit.type error
  })

  def setNBTItemSerializer(func: (ItemStack, NBTTagCompound) => Unit) = itemStackNBTSerializer.overrideDefault(func)

  def restoreDefaultNBTItemSerializer() = itemStackNBTSerializer.revert()

  def serialize(item: ItemStack, nbt: NBTTagCompound) = itemStackNBTSerializer.apply(item, nbt)

  def setNBTItemDeserializer(func: (NBTTagCompound) => ItemStack) = itemStackNBTDeserializer.overrideDefault(func)

  def restoreDefaultNBTItemDeserializer() = itemStackNBTDeserializer.revert()

  def deserialize(tag: NBTTagCompound): ItemStack = itemStackNBTDeserializer.apply(tag)
}

class NBTItemStorage(private val nbt: NBTTagCompound, isEmpty: Boolean = false) extends IItemStorage {
  private val access     = new StorageItemCollectionAccess(this)
  private val invWrapper = ItemAccessWrapperFactory.wrap(access)

  if (isEmpty)
    initializeEmptyNBT()

  override def getAccess: IItemCollectionAccess = access.synchronized(access)

  override def getInventory: IInventory = access.synchronized(invWrapper)

  override def loadFromNBT(compound: NBTTagCompound): Unit =
    access.synchronized {
                          incrementRevision()
                          initializeEmptyNBT()
                          compound.func_150296_c().asInstanceOf[java.util.Set[String]].foreach { key =>
                            nbt.setTag(key, compound.getTag(key).copy())
                                                                                               }
                        }

  override def saveToNBT(compound: NBTTagCompound): Unit =
    access.synchronized {
                          nbt.func_150296_c().asInstanceOf[java.util.Set[String]].foreach { key =>
                            compound.setTag(key, nbt.getTag(key).copy())
                                                                                          }
                        }

  override def getItemStack(slot: Int): Option[ItemStack] = Option(NBTItemStorage.deserialize(getItemCompound(slot)))

  override def setItemStack(slot: Int, item: ItemStack): Unit = {
    (item, getItemCompound(slot, force = item != null)) match {
      case (null, null) =>
      case (null, comp) => nbt.removeTag(slot.toString)
      case (i, _) =>
        val comp = new NBTTagCompound
        NBTItemStorage.serialize(i, comp)
        nbt.setTag(slot.toString, comp)
    }
  }

  private def getItemCompound(slot: Int, force: Boolean = false): NBTTagCompound = {
    val exists = nbt.hasKey(slot.toString)
    if (exists || force) {
      if (force && !exists) {
        nbt.setTag(slot.toString, new NBTTagCompound)
      }
      nbt.getCompoundTag(slot.toString)
    }
    else null
  }

  override def getSize: Int = nbt.getInteger(NBTItemStorage.SIZE_KEY)

  override def setSize(size: Int, clear: Boolean): Boolean =
    access.synchronized {
                          incrementRevision()
                          val itemKeys = nbt.func_150296_c().asInstanceOf[java.util.Set[String]].toSet
                          (if (clear)
                            itemKeys
                          else
                            itemKeys.filter(_.toInt >= size)
                            )
                          .foreach(nbt.removeTag)
                          nbt.setInteger(NBTItemStorage.SIZE_KEY, size)
                          true
                        }

  def initializeEmptyNBT() = {
    setSize(0, clear = true)
  }
}
