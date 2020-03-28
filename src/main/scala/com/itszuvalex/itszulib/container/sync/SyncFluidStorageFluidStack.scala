package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.api.storage.IFluidStorageModifiable
import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import com.itszuvalex.itszulib.util.Debug
import org.apache.logging.log4j.Level

/**
  * Created by Chris on 12/18/2016.
  */
class SyncFluidStorageFluidStack(gui: Int, val storage: IFluidStorageModifiable, protected var ind: Int, protected val playerInv: Boolean = false) extends SyncIFluidStack(gui, () => IFluidStack.Empty, (i: IFluidStack) => {}) {
  valueSetFunction = (i: IFluidStack) => {
    storage(storageIndex) = i
    Debug.log(Level.WARN, "Set storage stacksize:" + i.amount)
  }
  valueFunction = () => {
    storage(storageIndex)
  }

  def storageIndex: Int = ind

  def isPlayerInv: Boolean = playerInv

}
