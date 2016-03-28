package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.api.access.StorageFluidTankCollectionAccess
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidTank

import scala.collection.JavaConversions._

object NBTFluidTankStorage {
  val SIZE_KEY     = "Size"
  val CAPACITY_KEY = "Capacity"

  val fluidTankNBTDeserializer = new Overridable((n: NBTTagCompound) => {
    val tank = new FluidTank(n.getInteger(CAPACITY_KEY))
    tank.readFromNBT(n)
    tank
  })

  val fluidTankNBTSerializer = new Overridable((i: FluidTank, n: NBTTagCompound) => {
    i.writeToNBT(n)
    n.setInteger(CAPACITY_KEY, i.getCapacity)
  })

  def setNBTFluidSerializer(func: (FluidTank, NBTTagCompound) => Unit) = fluidTankNBTSerializer.overrideDefault(func)

  def restoreDefaultNBTFluidSerializer() = fluidTankNBTSerializer.revert()

  def serialize(fluid: FluidTank, nbt: NBTTagCompound) = fluidTankNBTSerializer.apply(fluid, nbt)

  def setNBTFluidDeserializer(func: (NBTTagCompound) => FluidTank) = fluidTankNBTDeserializer.overrideDefault(func)

  def restoreDefaultNBTFluidDeserializer() = fluidTankNBTDeserializer.revert()

  def deserialize(tag: NBTTagCompound): FluidTank = fluidTankNBTDeserializer.apply(tag)
}

class NBTFluidTankStorage(private val nbt: NBTTagCompound, isEmpty: Boolean = false) extends IFluidTankStorage {
  private val access = new StorageFluidTankCollectionAccess(this)

  if (isEmpty)
    initializeEmptyNBT()

  override def getFullAccess = access.synchronized(access)

  override def loadFromNBT(compound: NBTTagCompound): Unit =
    access.synchronized {
                          incrementRevision()
                          initializeEmptyNBT()
                          compound.func_150296_c().asInstanceOf[java.util.Set[String]].foreach { key =>
                            nbt.setTag(key, compound.getTag(key).copy())
                                                                                               }
                        }

  def initializeEmptyNBT() = {
    setSize(0, clear = true)
  }

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

  override def saveToNBT(compound: NBTTagCompound): Unit =
    access.synchronized {
                          nbt.func_150296_c().asInstanceOf[java.util.Set[String]].foreach { key =>
                            compound.setTag(key, nbt.getTag(key).copy())
                                                                                          }
                        }

  override def apply(slot: Int): FluidTank = NBTFluidTankStorage.deserialize(getFluidTankCompound(slot))

  override def update(slot: Int, fluid: FluidTank): Unit = {
    (fluid, getFluidTankCompound(slot, force = fluid != null)) match {
      case (null, null) =>
      case (null, comp) => nbt.removeTag(slot.toString)
      case (i, _) =>
        val comp = new NBTTagCompound
        NBTFluidTankStorage.serialize(i, comp)
        nbt.setTag(slot.toString, comp)
    }
  }

  private def getFluidTankCompound(slot: Int, force: Boolean = false): NBTTagCompound = {
    val exists = nbt.hasKey(slot.toString)
    if (exists || force) {
      if (force && !exists) {
        nbt.setTag(slot.toString, new NBTTagCompound)
      }
      nbt.getCompoundTag(slot.toString)
    }
    else null
  }

  override def length: Int = nbt.getInteger(NBTFluidTankStorage.SIZE_KEY)
}
