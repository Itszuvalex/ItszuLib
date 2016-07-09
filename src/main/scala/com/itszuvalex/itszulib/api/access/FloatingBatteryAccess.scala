package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.wrappers.IBattery

/**
  * Created by Chris on 7/8/2016.
  */
class FloatingBatteryAccess(battery: IBattery) extends IPowerAccess {
  private[access] var backingBattery = Option(battery)

  override def storage: Option[Double] = get.map(_.storage)

  override def maxStorage: Option[Double] = get.map(_.maxStorage)

  override def isValid: Boolean = true

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing Storage
    */
  override def get: Option[IBattery] = backingBattery

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  override def set(stack: IBattery): Unit = {
    backingBattery = Option(stack)
    super.set(stack)
  }
}
