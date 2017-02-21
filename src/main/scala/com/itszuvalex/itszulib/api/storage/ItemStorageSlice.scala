package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IItemStack
import com.itszuvalex.itszulib.util.Debug

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
class ItemStorageSlice(private var storage: IItemStorage, private var slots: Array[Int]) extends IItemStorage {
  Debug.only {
    slots.foreach { i =>
      Debug.assert(i >= 0 && i < storage.size, "Index in bounds.")
    }
  }

  /**
    *
    * @param i Index
    *
    * @return Get IItemStack contained at this location.  This should never return null, as IItemStacks track their own emptiness.
    */
  override def apply(i: Int): IItemStack = storage(slots(i))

  /**
    *
    * @param i Index to update
    * @param s IItemStack to set
    */
  override def update(i: Int, s: IItemStack): Unit = storage(slots(i)) = s

  override def length: Int = slots.length


}

