package com.itszuvalex.itszulib.api.core

import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Christopher Harris (Itszuvalex) on 4/6/15.
  */
trait NBTSerializable {
  def saveToNBT(compound: NBTTagCompound)

  def loadFromNBT(compound: NBTTagCompound)
}
