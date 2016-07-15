package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

/**
  * Created by Chris on 7/8/2016.
  */
object IBattery {
  def createFromNBT(nbt: NBTTagCompound): IBattery = {
    val battery = PowerBattery(0, 0)
    battery.deserializeNBT(nbt)
    battery
  }
}

trait IBattery extends INBTSerializable[NBTTagCompound] {

  def storage: Double

  def storage_=(amt: Double): Unit

  def maxStorage: Double

  def maxStorage_=(max: Double): Unit

  def copy(): IBattery

  def clear(): Unit

  def writeToNBT(nbt: NBTTagCompound): Unit
}
