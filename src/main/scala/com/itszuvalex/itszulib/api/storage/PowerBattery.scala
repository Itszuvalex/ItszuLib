package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.storage.nbt.NBTInstanceSerializable
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 7/8/2016.
  */
case class PowerBattery[N : Numeric : NBTInstanceSerializable](private var power: N, private var powerMax: N) extends IBattery[N] {
  val numeric: Numeric[N] = implicitly[Numeric[N]]

  override def amount: N = power

  override def amount_=(amount: N): Unit = power = numeric.min(amountMax, amount)

  override def amountMax: N = powerMax

  override def clear(): Unit = power = numeric.zero

  override def copy(): IBattery[N] = PowerBattery[N](power, powerMax)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    val wrap = new PowerBatteryNBT[N](nbt)
    powerMax = wrap.amountMax
    power = wrap.amount
  }

  override def writeToNBT(nbt: NBTTagCompound): Unit = {
    val wrap = new PowerBatteryNBT[N](nbt)
    wrap.amountMax = amountMax
    wrap.amount = amount
    wrap.writeToNBT(nbt)
  }
}
