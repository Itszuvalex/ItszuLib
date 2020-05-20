package com.itszuvalex.itszulib.util

import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.IItemStack

object StorageUtils {

  def storageContainsItems(storage: IItemStorage, items: Iterable[IItemStack]): Boolean =
    removeItemsFromStorageInternal(storage, items, doRemove = false, removeIfNotAllFound = false)

  private def removeItemsFromStorageInternal(storage: IItemStorage, items: Iterable[IItemStack], doRemove: Boolean, removeIfNotAllFound: Boolean): Boolean = {
    val removed = new Array[Int](storage.length)
    var removedAll = true

    def getAmtInSlot(i: Int) = storage(i).amount - removed(i)

    def removeAmtInSlot(slot: Int, amt: Int): Int = {
      val remaining = getAmtInSlot(slot)
      if (remaining > amt) {
        removed(slot) += amt
        0
      }
      else {
        removed(slot) += remaining
        amt - remaining
      }
    }

    items.filterNot(_.isEmpty).foreach { stack =>
      var toRemove = stack.amount
      removedAll = storage.zipWithIndex.exists { store =>
        toRemove = removeAmtInSlot(store._2, toRemove)
        toRemove <= 0
      } && removedAll
    }

    if (doRemove) {
      if (removedAll || removeIfNotAllFound) {
        storage.indices.foreach { i =>
          storage.split(i, removed(i))
        }
      }
    }

    removedAll || removeIfNotAllFound
  }

  def removeItemsFromStorage(storage: IItemStorage, items: Iterable[IItemStack], removeIfNotAllFound: Boolean = false): Boolean =
    removeItemsFromStorageInternal(storage, items, doRemove = true, removeIfNotAllFound = removeIfNotAllFound)

}
