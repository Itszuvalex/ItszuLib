package com.itszuvalex.itszulib.container.sync

import net.minecraft.nbt.{NBTBase, NBTTagFloat}

/**
  * Created by Chris on 12/13/2016.
  */
class SyncFloat(gui: Int, sync: () => Float, write: (Float) => Unit) extends SyncBase[Float](gui, sync, write, (a: Float, b: Float) => a == b) {
  override def writeNBT(): NBTBase = new NBTTagFloat(value)

  override def handleNBT(nbt: NBTBase): Unit = nbt match {
    case null => value = 0
    case nbti: NBTTagFloat => value = nbti.getFloat
    case _ => value = 0
  }
}
