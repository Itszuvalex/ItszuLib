package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
class ItemStorageIterator(private val storage: IItemStorage) extends Iterator[IItemStack] {
  var index = 0

  override def hasNext: Boolean = index < storage.length

  override def next(): IItemStack = {
    val ret = storage(index)
    index += 1
    ret
  }
}
