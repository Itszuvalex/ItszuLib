package com.itszuvalex.itszulib.api.storage

import net.minecraft.nbt.NBTTagCompound

class DynamicIBattery(val getter: () => IBattery) extends IBattery {
  override def storage: Double = getter().storage

  override def storage_=(amt: Double): Unit = getter().storage_=(amt)

  override def maxStorage: Double = getter().maxStorage

  override def maxStorage_=(max: Double): Unit = getter().maxStorage_=(max)

  override def copy(): IBattery = getter().copy()

  override def clear(): Unit = getter().clear()

  override def writeToNBT(nbt: NBTTagCompound): Unit = getter().writeToNBT(nbt)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = getter().deserializeNBT(nbt)

  override def serializeNBT(): NBTTagCompound = getter().serializeNBT()
}
