package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.IItemStack

/**
  * Created by Chris on 12/18/2016.
  */
class SyncItemStorageItemStack(val storage: IItemStorage, protected var ind: Int) extends SyncIItemStack(() => IItemStack.Empty, (i: IItemStack) => {}) {
  valueSetFunction = (i: IItemStack) => storage(storageIndex) = i
  valueFunction = () => storage(storageIndex)

  def storageIndex: Int = ind

}
