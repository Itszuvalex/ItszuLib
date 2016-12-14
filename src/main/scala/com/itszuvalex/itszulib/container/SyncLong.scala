package com.itszuvalex.itszulib.container

import net.minecraft.nbt.{NBTBase, NBTTagLong}

/**
  * Created by Chris on 12/13/2016.
  */
class SyncLong(sync: () => Long, write: (Long) => Unit) extends SyncBase[Long](sync, write) {
  override def writeNBT(): NBTBase = new NBTTagLong(value)

  override def handleNBT(nbt: NBTBase): Unit = nbt match {
    case nbti: NBTTagLong => value = nbti.getLong
    case _ => value = 0
  }
}
