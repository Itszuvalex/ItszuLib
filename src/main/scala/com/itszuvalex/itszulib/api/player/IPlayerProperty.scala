package com.itszuvalex.itszulib.api.player

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

trait IPlayerProperty extends INBTSerializable[NBTTagCompound] {
  def saveToDescPacket(nbt: NBTTagCompound): Unit

  def loadFromDescPacket(nbt: NBTTagCompound): Unit
}
