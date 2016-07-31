package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
class ItemStorageArray(private var storage: Array[IItemStack]) extends IItemStorage {
  // Memory already allocated, might as well just prefill to default value
  storage.indices.filter(storage(_) == null).foreach(storage(_) = IItemStack.Empty)

  def this(size: Int) = this(new Array[IItemStack](size))

  def this() = this(0)

  /**
    *
    * @param i Index
    * @return Get IItemStack contained at this location.  This should never return null, as IItemStacks track their own emptiness.
    */
  override def apply(i: Int): IItemStack = storage(i)

  /**
    *
    * @param i Index to update
    * @param s IItemStack to set
    */
  override def update(i: Int, s: IItemStack): Unit = storage(i) = s

  override def length: Int = storage.length


}

