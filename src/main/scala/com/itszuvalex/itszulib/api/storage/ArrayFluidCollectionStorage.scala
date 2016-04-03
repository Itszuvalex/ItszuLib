package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IFluidCollectionAccess, StorageFluidCollectionAccess}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/16.
  */
class ArrayFluidCollectionStorage(private var array: Array[FluidStack]) extends IFluidCollectionStorage {
  private val access = new StorageFluidCollectionAccess(this)

  def this(size: Int) = this(new Array[FluidStack](size))

  def this() = this(0)

  override def getFullAccess: IFluidCollectionAccess = access.synchronized(access)

  override def serializeNBT(): NBTTagCompound =
    access.synchronized {
                          val nbt = new NBTTagCompound
                          val store = new NBTFluidCollectionStorage(nbt, true)
                          store.setSize(length, clear = true)
                          store.getFullAccess.copyFromAccess(access, copy = false)
                          nbt
                        }

  override def length: Int = array.length

  override def deserializeNBT(compound: NBTTagCompound): Unit =
    access.synchronized {
                          val nbt = new NBTFluidCollectionStorage(compound, false)
                          updateBackingStore(new Array[FluidStack](nbt.length))
                          access.copyFromAccess(nbt.getFullAccess, copy = false)
                        }

  /**
    *
    * @param size  New size to set to.  This will trim elements if size is smaller than current size.
    *              Otherwise, it will pad default elements. (Nulls)
    * @param clear Set to true to null out the array before the resize.  This is independent of trimming.
    * @return
    */
  override def setSize(size: Int, clear: Boolean): Boolean = {
    if (clear)
      updateBackingStore(new Array[FluidStack](size))
    else {
      updateBackingStore(java.util.Arrays.copyOf(array, size))
    }
    true
  }

  private def updateBackingStore(newArray: Array[FluidStack]): Unit = {
    access.synchronized {
                          incrementRevision()
                          array = newArray
                        }
  }

  override def update(slot: Int, value: FluidStack): Unit = array(slot) = value

  override def apply(slot: Int): FluidStack = array(slot)
}
