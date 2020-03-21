package com.itszuvalex.itszulib.container.sync

import net.minecraft.nbt.{NBTBase, NBTTagLong}

/**
  * Created by Chris on 12/13/2016.
  */
class SyncLong(gui: Int, sync: () => Long, write: (Long) => Unit) extends SyncBase[Long](gui, sync, write, (a: Long, b: Long) => a == b) {
  override def writeNBT(): NBTBase = new NBTTagLong(value)

  override def handleNBT(nbt: NBTBase): Unit = nbt match {
    case null => value = 0
    case nbti: NBTTagLong => value = nbti.getLong
    case _ => value = 0
  }
}
