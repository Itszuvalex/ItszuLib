package com.itszuvalex.itszulib.container.sync

import net.minecraft.nbt.{NBTBase, NBTTagCompound}

object SyncStringArray {
  val SIZE_KEY = "size"

  def storageEquals(a: Array[String], b: Array[String]): Boolean = {
    if (a == null && b == null) return true
    if ((a == null) != (b == null)) return false
    if (a.length != b.length) return false

    a.indices.forall(i => a(i).equalsIgnoreCase(b(i)))
  }
}

class SyncStringArray
(gui: Int, valFunc: () => Array[String], setValFunc: (Array[String]) => Unit)
  extends SyncBase[Array[String]](gui, valFunc, setValFunc, SyncStringArray.storageEquals) {
  override def writeNBT(): NBTBase = {
    val tag = new NBTTagCompound
    tag.setInteger(SyncStringArray.SIZE_KEY, value.length)
    value.indices.foreach(i => tag.setString(i.toString, value(i)))
    tag
  }

  override def handleNBT(nbt: NBTBase): Unit = {
    val array = new Array[String](nbt.asInstanceOf[NBTTagCompound].getInteger(SyncStringArray.SIZE_KEY))
    array.indices.foreach(i => array(i) = nbt.asInstanceOf[NBTTagCompound].getString(i.toString))
    value = array
  }
}
