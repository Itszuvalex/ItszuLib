package com.itszuvalex.itszulib.api.storage

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

trait IBattery[N] extends IStack[N] with INBTSerializable[NBTTagCompound] {
  def copy(): IBattery[N]

  def clear(): Unit

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    writeToNBT(nbt)
    nbt
  }

  def writeToNBT(nbt: NBTTagCompound): Unit
}
