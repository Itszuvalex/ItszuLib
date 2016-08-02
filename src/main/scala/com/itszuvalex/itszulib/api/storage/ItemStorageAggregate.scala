package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IItemStack

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Christopher Harris (Itszuvalex) on 7/31/16.
  */
class ItemStorageAggregate(private var storages: ArrayBuffer[IItemStorage]) extends IItemStorage {
  /**
    *
    * @param i Index
    * @return Get IItemStack contained at this location.  This should never return null, as IItemStacks track their own emptiness.
    */
  override def apply(i: Int): IItemStack = getStorageForIndex(i).map(_.apply(getStorageIndexForIndex(i))).getOrElse(IItemStack.Empty)

  /**
    *
    * @param i Index to update
    * @param s IItemStack to set
    */
  override def update(i: Int, s: IItemStack): Unit = {
    getStorageForIndex(i).foreach(_.update(getStorageIndexForIndex(i), s))
  }

  override def length: Int = storages.foldLeft(0)((sum, storage) => sum + storage.length)

  def getStorageForIndex(index: Int): Option[IItemStorage] = {
    var i = index
    storages.find {
                    storage =>
                      if (i < storage.length)
                        true
                      else {
                        i -= storage.length
                        false
                      }
                  }
  }

  def getStorageIndexForIndex(index: Int): Int = {
    var i = index
    storages.foreach { storage =>
      if (i < storage.length)
        return i
      else {
        i -= storage.length
      }
                     }
    0
  }
}
