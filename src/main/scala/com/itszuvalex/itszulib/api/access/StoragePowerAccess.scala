package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.storage.IPowerCollectionStorage
import com.itszuvalex.itszulib.api.wrappers.IBattery

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
class StoragePowerAccess(private val powStorage: IPowerCollectionStorage, private val index: Int) extends IPowerAccess {
  private val revision = powStorage.getRevision

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  override def get: Option[IBattery] = if (isValid) Option(powStorage(index)) else None

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = revision == powStorage.getRevision

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  override def set(stack: IBattery): Unit = {
    powStorage(index) = stack
    onChanged()
  }

  /**
    * Call when this changes backing item.
    */
  override def onChanged(): Unit = powStorage.onChanged(index)
}
