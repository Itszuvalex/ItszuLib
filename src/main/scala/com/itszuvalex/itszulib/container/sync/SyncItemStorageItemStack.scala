package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.IItemStack
import com.itszuvalex.itszulib.util.Debug
import org.apache.logging.log4j.Level

/**
  * Created by Chris on 12/18/2016.
  */
class SyncItemStorageItemStack(gui: Int, val storage: IItemStorage, protected var ind: Int, protected val playerInv: Boolean = false) extends SyncIItemStack(gui, () => IItemStack.Empty, (i: IItemStack) => {}) {
  valueSetFunction = (i: IItemStack) => {
    storage(storageIndex) = i
    Debug.log(Level.WARN, "Set storage stacksize:" + i.stackSize)
  }
  valueFunction = () => {
    storage(storageIndex)
  }

  def storageIndex: Int = ind

  def isPlayerInv: Boolean = playerInv

}
