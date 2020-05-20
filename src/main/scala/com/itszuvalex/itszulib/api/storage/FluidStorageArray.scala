package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack

class FluidStorageArray(val storage: Array[IFluidStack], val cap: mL) extends IFluidStorageModifiable {
  // Memory already allocated, might as well just prefill to default value
  storage.indices.withFilter(storage(_) == null).foreach(storage(_) = IFluidStack.Empty)

  def this(size: Int, capacity: mL) = this(new Array[IFluidStack](size), capacity)

  def this(s: Array[IFluidStack]) = this(s, Int.MaxValue)

  def this() = this(0, Int.MaxValue)

  /**
    *
    * @param i Index
    * @return Get IFluidStack contained at this location.  This should never return null, as IFluidStacks track their own emptiness.
    */
  override def apply(i: Int): IFluidStack = storage(i)

  /**
    *
    * @param i Index to update
    * @param s IFluidStack to set
    */
  override def update(i: Int, s: IFluidStack): Unit = storage(i) = s

  override def length: Int = storage.length

  override def capacity(index: Int): Int = cap
}
