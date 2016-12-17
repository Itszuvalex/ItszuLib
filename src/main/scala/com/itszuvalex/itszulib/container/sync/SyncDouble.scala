package com.itszuvalex.itszulib.container.sync

import net.minecraft.nbt.{NBTBase, NBTTagDouble}

/**
  * Created by Chris on 12/13/2016.
  */
class SyncDouble(sync: () => Double, write: (Double) => Unit) extends SyncBase[Double](sync, write) {
  override def writeNBT(): NBTBase = new NBTTagDouble(value)

  override def handleNBT(nbt: NBTBase): Unit = nbt match {
    case nbti: NBTTagDouble => value = nbti.getDouble
    case _ => value = 0
  }
}
