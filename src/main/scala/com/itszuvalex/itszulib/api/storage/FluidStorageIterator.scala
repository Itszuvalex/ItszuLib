package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack

class FluidStorageIterator(private val storage: IFluidStorage) extends Iterator[IFluidStack] {
  var index = 0

  override def hasNext: Boolean = index < storage.length

  override def next(): IFluidStack = {
    val ret = storage(index)
    index += 1
    ret
  }
}
