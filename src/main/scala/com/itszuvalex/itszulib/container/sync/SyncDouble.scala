package com.itszuvalex.itszulib.container.sync

import net.minecraft.nbt.{NBTBase, NBTTagDouble}

/**
  * Created by Chris on 12/13/2016.
  */
class SyncDouble(gui: Int, sync: () => Double, write: (Double) => Unit) extends SyncBase[Double](gui, sync, write, (a: Double, b: Double) => a == b) {
  override def writeNBT(): NBTBase = new NBTTagDouble(value)

  override def handleNBT(nbt: NBTBase): Unit = nbt match {
    case nbti: NBTTagDouble => value = nbti.getDouble
    case _ => value = 0
  }
}
