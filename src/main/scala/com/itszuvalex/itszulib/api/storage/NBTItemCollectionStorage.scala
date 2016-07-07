package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.api.access.{IItemCollectionAccess, ItemAccessWrapperFactory, StorageItemCollectionAccess}
import com.itszuvalex.itszulib.api.wrappers.IItemStack
import net.minecraft.inventory.IInventory
import net.minecraft.nbt.NBTTagCompound

import scala.collection.JavaConversions._

/**
  * Created by Christopher Harris (Itszuvalex) on 3/23/16.
  */
object NBTItemCollectionStorage {
  val SIZE_KEY = "Size"

  val itemStackNBTDeserializer = new Overridable(IItemStack.createFromNBT _)
  val itemStackNBTSerializer   = new Overridable((i: IItemStack, n: NBTTagCompound) => {
    Option(i).foreach(_.writeToNBT(n))
    () // Fix Unit.type error
  })

  def setNBTItemSerializer(func: (IItemStack, NBTTagCompound) => Unit) = itemStackNBTSerializer.overrideDefault(func)

  def restoreDefaultNBTItemSerializer() = itemStackNBTSerializer.revert()

  def serialize(item: IItemStack, nbt: NBTTagCompound) = itemStackNBTSerializer.apply(item, nbt)

  def setNBTItemDeserializer(func: (NBTTagCompound) => IItemStack) = itemStackNBTDeserializer.overrideDefault(func)

  def restoreDefaultNBTItemDeserializer() = itemStackNBTDeserializer.revert()

  def deserialize(tag: NBTTagCompound) = itemStackNBTDeserializer.apply(tag)
}

class NBTItemCollectionStorage(private val nbt: NBTTagCompound, isEmpty: Boolean = false) extends IItemCollectionStorage {
  private val access     = new StorageItemCollectionAccess(this)
  private val invWrapper = ItemAccessWrapperFactory.wrap(access)

  if (isEmpty)
    initializeEmptyNBT()

  override def getFullAccess: IItemCollectionAccess = access.synchronized(access)

  override def getInventory: IInventory = access.synchronized(invWrapper)

  override def deserializeNBT(compound: NBTTagCompound): Unit =
    access.synchronized {
                          incrementRevision()
                          initializeEmptyNBT()
                          compound.getKeySet.foreach { key =>
                            nbt.setTag(key, compound.getTag(key).copy())
                                                     }
                        }

  def initializeEmptyNBT() = {
    setSize(0, clear = true)
  }

  override def setSize(size: Int, clear: Boolean): Boolean =
    access.synchronized {
                          incrementRevision()
                          val itemKeys = nbt.getKeySet.toSet
                          (if (clear)
                            itemKeys
                          else
                            itemKeys.filter(_.toInt >= size)
                            )
                          .foreach(nbt.removeTag)
                          nbt.setInteger(NBTItemCollectionStorage.SIZE_KEY, size)
                          true
                        }

  override def serializeNBT(): NBTTagCompound =
    access.synchronized {
                          val compound = new NBTTagCompound
                          nbt.getKeySet.foreach { key =>
                            compound.setTag(key, nbt.getTag(key).copy())
                                                }
                          compound
                        }

  override def apply(slot: Int): IItemStack = NBTItemCollectionStorage.deserialize(getItemCompound(slot))

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

  override def update(slot: Int, item: IItemStack): Unit = {
    (item, getItemCompound(slot, force = item != null)) match {
      case (null, null) =>
      case (null, comp) => nbt.removeTag(slot.toString)
      case (i, _) =>
        val comp = new NBTTagCompound
        NBTItemCollectionStorage.serialize(i, comp)
        nbt.setTag(slot.toString, comp)
    }
  }

  override def length: Int = nbt.getInteger(NBTItemCollectionStorage.SIZE_KEY)
}
