package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 7/8/2016.
  */
case class PowerBattery(private var power: Double, private var powerMax: Double) extends IBattery {

  def this(max: Double) = this(0, max)

  override def storage: Double = power

  override def maxStorage: Double = powerMax

  override def maxStorage_=(max: Double): Unit = powerMax = max

  override def storage_=(amt: Double): Unit = power = Math.max(maxStorage, amt)

  override def clear(): Unit = power = 0

  override def copy(): IBattery = PowerBattery(power, powerMax)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    val wrap = new WrapperNBTBattery(nbt)
    powerMax = wrap.maxStorage
    power = wrap.storage
  }

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    writeToNBT(nbt)
    nbt
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    val wrap = new WrapperNBTBattery(nbt)
    wrap.maxStorage = maxStorage
    wrap.storage = storage
    wrap.writeToNBT(nbt)
  }
}
