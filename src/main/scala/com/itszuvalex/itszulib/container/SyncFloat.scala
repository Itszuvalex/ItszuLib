package com.itszuvalex.itszulib.container

import net.minecraft.nbt.{NBTBase, NBTTagFloat}

/**
  * Created by Chris on 12/13/2016.
  */
class SyncFloat(sync: () => Float, write: (Float) => Unit) extends SyncBase[Float](sync, write) {
  override def writeNBT(): NBTBase = new NBTTagFloat(value)

  override def handleNBT(nbt: NBTBase): Unit = nbt match {
    case nbti: NBTTagFloat => value = nbti.getFloat
    case _ => value = 0
  }
}
