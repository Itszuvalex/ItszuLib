package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.api.access.{IPowerCollectionAccess, StoragePowerCollectionAccess}
import com.itszuvalex.itszulib.api.wrappers.IBattery
import net.minecraft.nbt.NBTTagCompound

import scala.collection.JavaConversions._


object NBTPowerCollectionStorage {
  val SIZE_KEY = "Size"

  val powerNBTDeserializer = new Overridable(IBattery.createFromNBT _)
  val powerNBTSerializer   = new Overridable((i: IBattery, n: NBTTagCompound) => {
    Option(i).foreach(_.writeToNBT(n))
    () // Fix Unit.type error
  })

  def setNBTPowerSerializer(func: (IBattery, NBTTagCompound) => Unit) = powerNBTSerializer.overrideDefault(func)

  def restoreDefaultNBTPowerSerializer() = powerNBTSerializer.revert()

  def serialize(item: IBattery, nbt: NBTTagCompound) = powerNBTSerializer.apply(item, nbt)

  def setNBTPowerDeserializer(func: (NBTTagCompound) => IBattery) = powerNBTDeserializer.overrideDefault(func)

  def restoreDefaultNBTPowerDeserializer() = powerNBTDeserializer.revert()

  def deserialize(tag: NBTTagCompound) = powerNBTDeserializer.apply(tag)
}

class NBTPowerCollectionStorage(private val nbt: NBTTagCompound, isEmpty: Boolean = false) extends IPowerCollectionStorage {
  private val access = new StoragePowerCollectionAccess(this)

  if (isEmpty)
    initializeEmptyNBT()

  override def getFullAccess: IPowerCollectionAccess = access.synchronized(access)

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
                          nbt.setInteger(NBTPowerCollectionStorage.SIZE_KEY, size)
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

  override def apply(slot: Int): IBattery = NBTPowerCollectionStorage.deserialize(getPowerCompound(slot))

  private def getPowerCompound(slot: Int, force: Boolean = false): NBTTagCompound = {
    val exists = nbt.hasKey(slot.toString)
    if (exists || force) {
      if (force && !exists) {
        nbt.setTag(slot.toString, new NBTTagCompound)
      }
      nbt.getCompoundTag(slot.toString)
    }
    else null
  }

  override def update(slot: Int, item: IBattery): Unit = {
    (item, getPowerCompound(slot, force = item != null)) match {
      case (null, null) =>
      case (null, comp) => nbt.removeTag(slot.toString)
      case (i, _) =>
        val comp = new NBTTagCompound
        NBTPowerCollectionStorage.serialize(i, comp)
        nbt.setTag(slot.toString, comp)
    }
  }

  override def length: Int = nbt.getInteger(NBTPowerCollectionStorage.SIZE_KEY)
}
