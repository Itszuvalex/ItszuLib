package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import com.itszuvalex.itszulib.util.Debug
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
class FluidStorageSlice(private var storage: IFluidStorage, private var slots: Array[Int]) extends IFluidStorage {
  Debug.only {
    slots.foreach { i =>
      Debug.assert(i >= 0 && i < storage.size, "Index in bounds.")
    }
  }

  /**
    *
    * @param i Index
    *
    * @return Get IFluidStack contained at this location.  This should never return null, as IFluidStacks track their own emptiness.
    */
  override def apply(i: Int): IFluidStack = storage(slots(i))

  override def length: Int = slots.length

  override def fill(resource: IFluidStack, doFill: Boolean): Int = ???

  override def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = ???

  override def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = ???

  override def serializeNBT(): NBTTagCompound = ???

  override def deserializeNBT(nbt: NBTTagCompound): Unit = ???
}

