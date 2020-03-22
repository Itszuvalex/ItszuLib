package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.storage.PowerBatteryNBT._
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 12/11/2016.
  */

object PowerBatteryNBT {
  val POWER_TAG     = "POWER"
  val POWER_MAX_TAG = "POWER_MAX"
}

class PowerBatteryNBT(private var nbt: NBTTagCompound) extends IBattery {
  override def storage: Double = nbt.getDouble(POWER_TAG)

  override def storage_=(amt: Double): Unit = nbt.setDouble(POWER_TAG, Math.min(maxStorage, amt))

  override def maxStorage: Double = nbt.getDouble(POWER_MAX_TAG)

  override def maxStorage_=(max: Double): Unit = nbt.setDouble(POWER_MAX_TAG, max)

  override def copy(): IBattery = PowerBattery(storage, maxStorage)

  override def clear(): Unit = {
    storage = 0
    maxStorage = 0
  }

  override def writeToNBT(nbt: NBTTagCompound): Unit = {
    nbt.setDouble(POWER_TAG, storage)
    nbt.setDouble(POWER_MAX_TAG, maxStorage)
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    this.nbt = nbt.copy()
  }

  override def serializeNBT(): NBTTagCompound = {
    nbt.copy()
  }
}
