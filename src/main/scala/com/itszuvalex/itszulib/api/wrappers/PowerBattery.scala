package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 7/8/2016.
  */
object PowerBattery {
  val POWER_TAG     = "POWER"
  val POWER_MAX_TAG = "POWER_MAX"
}

case class PowerBattery(private var power: Double, private var powerMax: Double) extends IBattery {

  def this(max: Double) = this(0, max)

  override def storage: Double = power

  override def maxStorage: Double = powerMax

  override def maxStorage_=(max: Double): Unit = powerMax = max

  override def storage_=(amt: Double): Unit = power = Math.max(maxStorage, amt)

  override def clear(): Unit = power = 0

  override def copy(): IBattery = PowerBattery(power, powerMax)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    power = nbt.getDouble(PowerBattery.POWER_TAG)
    powerMax = nbt.getDouble(PowerBattery.POWER_MAX_TAG)
  }

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    writeToNBT(nbt)
    nbt
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    nbt.setDouble(PowerBattery.POWER_TAG, power)
    nbt.setDouble(PowerBattery.POWER_MAX_TAG, powerMax)
  }
}
