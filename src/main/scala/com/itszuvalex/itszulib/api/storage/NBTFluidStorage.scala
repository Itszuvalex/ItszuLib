package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.api.access.StorageFluidCollectionAccess
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidStack

import scala.collection.JavaConversions._

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/2016.
  */
object NBTFluidStorage {
  val SIZE_KEY = "Size"

  val fluidStackNBTDeserializer = new Overridable(FluidStack.loadFluidStackFromNBT _)
  val fluidStackNBTSerializer   = new Overridable((i: FluidStack, n: NBTTagCompound) => {
    i.writeToNBT(n)
    () // Fix Unit.type error
  })

  def setNBTFluidSerializer(func: (FluidStack, NBTTagCompound) => Unit) = fluidStackNBTSerializer.overrideDefault(func)

  def restoreDefaultNBTFluidSerializer() = fluidStackNBTSerializer.revert()

  def serialize(fluid: FluidStack, nbt: NBTTagCompound) = fluidStackNBTSerializer.apply(fluid, nbt)

  def setNBTFluidDeserializer(func: (NBTTagCompound) => FluidStack) = fluidStackNBTDeserializer.overrideDefault(func)

  def restoreDefaultNBTFluidDeserializer() = fluidStackNBTDeserializer.revert()

  def deserialize(tag: NBTTagCompound): FluidStack = fluidStackNBTDeserializer.apply(tag)
}

class NBTFluidStorage(private val nbt: NBTTagCompound, isEmpty: Boolean = false) extends IFluidStorage {
  private val access = new StorageFluidCollectionAccess(this)

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

  override def apply(slot: Int): FluidStack = NBTFluidStorage.deserialize(getFluidCompound(slot))

  override def update(slot: Int, fluid: FluidStack): Unit = {
    (fluid, getFluidCompound(slot, force = fluid != null)) match {
      case (null, null) =>
      case (null, comp) => nbt.removeTag(slot.toString)
      case (i, _) =>
        val comp = new NBTTagCompound
        NBTFluidStorage.serialize(i, comp)
        nbt.setTag(slot.toString, comp)
    }
  }

  private def getFluidCompound(slot: Int, force: Boolean = false): NBTTagCompound = {
    val exists = nbt.hasKey(slot.toString)
    if (exists || force) {
      if (force && !exists) {
        nbt.setTag(slot.toString, new NBTTagCompound)
      }
      nbt.getCompoundTag(slot.toString)
    }
    else null
  }

  override def length: Int = nbt.getInteger(NBTFluidStorage.SIZE_KEY)
}
