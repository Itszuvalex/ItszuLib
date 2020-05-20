package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.storage.PowerBatteryNBT._
import com.itszuvalex.itszulib.api.storage.nbt.NBTInstanceSerializable
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 12/11/2016.
  */

object PowerBatteryNBT {
  val POWER_TAG     = "POWER"
  val POWER_MAX_TAG = "POWER_MAX"
}

class PowerBatteryNBT[N : NBTInstanceSerializable : Numeric](private var nbt: NBTTagCompound) extends IBattery[N] {
  val nbts: NBTInstanceSerializable[N] = implicitly[NBTInstanceSerializable[N]]
  val numeric: Numeric[N] = implicitly[Numeric[N]]

  override def amount: N = nbts.deserializeInstance(nbt, POWER_TAG)

  override def amount_=(amount: N): Unit = nbts.serialize(numeric.min(amountMax, amount), nbt, POWER_TAG)

  override def amountMax: N = nbts.deserializeInstance(nbt, POWER_MAX_TAG)

  def amountMax_=(amountMax: N): Unit = nbts.serialize(amountMax, nbt, POWER_MAX_TAG)

  override def copy(): IBattery[N] = PowerBattery(amount, amountMax)

  override def clear(): Unit = {
    amount = numeric.zero
  }

  override def writeToNBT(nbt: NBTTagCompound): Unit = {
    nbts.serialize(amount, nbt, POWER_TAG)
    nbts.serialize(amountMax, nbt, POWER_MAX_TAG)
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    this.nbt = nbt.copy()
  }

  override def serializeNBT(): NBTTagCompound = {
    nbt.copy()
  }
}
