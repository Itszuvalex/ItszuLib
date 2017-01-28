package com.itszuvalex.itszulib.container.sync

import net.minecraft.nbt.{NBTBase, NBTTagInt}

/**
  * Created by Chris on 12/13/2016.
  */
class SyncInt(gui: Int, sync: () => Int, write: (Int) => Unit) extends SyncBase[Int](gui, sync, write) {
  override def writeNBT(): NBTBase = new NBTTagInt(value)

  override def handleNBT(nbt: NBTBase): Unit = nbt match {
    case nbti: NBTTagInt => value = nbti.getInt
    case _ => value = 0
  }
}
