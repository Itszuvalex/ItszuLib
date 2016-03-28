package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IFluidTankCollectionAccess, StorageFluidTankCollectionAccess}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidTank

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/16.
  */
class ArrayFluidTankCollectionStorage(private var array: Array[FluidTank]) extends IFluidTankCollectionStorage {
  private val access = new StorageFluidTankCollectionAccess(this)

  def this(size: Int) = this(new Array[FluidTank](size))

  def this() = this(0)

  override def getFullAccess: IFluidTankCollectionAccess = access.synchronized(access)

  override def saveToNBT(compound: NBTTagCompound) =
    access.synchronized {
                          val store = new NBTFluidTankCollectionStorage(compound, true)
                          store.setSize(length, clear = true)
                          store.getFullAccess.copyFromAccess(access, copy = false)
                        }

  override def length: Int = array.length

  override def loadFromNBT(compound: NBTTagCompound) =
    access.synchronized {
                          val nbt = new NBTFluidTankCollectionStorage(compound, false)
                          updateBackingStore(new Array[FluidTank](nbt.length))
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
      updateBackingStore(new Array[FluidTank](size))
    else {
      updateBackingStore(java.util.Arrays.copyOf(array, size))
    }
    true
  }

  private def updateBackingStore(newArray: Array[FluidTank]): Unit = {
    access.synchronized {
                          incrementRevision()
                          array = newArray
                        }
  }

  override def update(slot: Int, value: FluidTank): Unit = array(slot) = value

  override def apply(slot: Int): FluidTank = array(slot)
}
