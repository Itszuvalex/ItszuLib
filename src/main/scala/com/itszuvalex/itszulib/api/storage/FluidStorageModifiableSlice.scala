package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import com.itszuvalex.itszulib.util.Debug

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
class FluidStorageModifiableSlice(private var storage: IFluidStorageModifiable, private var slots: Array[Int]) extends IFluidStorageModifiable {
  Debug.only {
    slots.foreach { i =>
      Debug.assert(i >= 0 && i < storage.size, "Index in bounds.")
    }
  }

  /**
    *
    * @param i Index
    * @return Get IFluidStack contained at this location.  This should never return null, as IFluidStacks track their own emptiness.
    */
  override def apply(i: Int): IFluidStack = storage(slots(i))

  override def length: Int = slots.length

  override def update(i: Int, s: IFluidStack): Unit = storage.update(slots(i), s)
}

