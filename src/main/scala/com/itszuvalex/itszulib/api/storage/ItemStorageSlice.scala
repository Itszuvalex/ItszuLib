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

  /**
    * Does not actually prevent inserting.  This should be overridden by inventories that want to prevent insertion by players.
    * Insertion by machines should also check this, but in cases of output slots we can't just prevent insertion of items at the storage level.
    *
    * @param i     Index
    * @param stack Stack to insert
    *
    * @return True if this stack can be inserted.
    */
  override def canInsert(i: Int, stack: IItemStack): Boolean = storage.canInsert(slots(i), stack)

  /**
    *
    * @return Max stack size allowed in this storage.
    */
  override def maxStackSize(i: Int): Int = storage.maxStackSize(slots(i))

  /**
    *
    * @param i Index to split
    * @param a Amount to attempt to pull out of this location.
    *
    * @return IItemStack containing the split stack.  This should never return null.
    */
  override def split(i: Int, a: Int): IItemStack = storage.split(slots(i), a)

  /**
    *
    * @param i Index to insert into
    * @param s ItemStack to attempt to insert.
    *
    * @return IItemStack containing the leftovers from s.  This should never return null.
    */
  override def insert(i: Int, s: IItemStack): IItemStack = storage.insert(slots(i), s)

  /**
    *
    * @param i Index
    * @param s ItemStack to set index = to
    */
  override def setSlot(i: Int, s: IItemStack): Unit = storage.setSlot(slots(i), s)
}

