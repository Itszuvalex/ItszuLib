package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.core.EnumAutomaticIO
import net.minecraft.nbt.{NBTBase, NBTTagCompound}

object SyncEnumAutomaticIOArray {
  val SIZE_KEY = "size"

  def storageEquals(a: Array[EnumAutomaticIO], b: Array[EnumAutomaticIO]): Boolean = {
    if (a == null && b == null) return true
    if ((a == null) != (b == null)) return false
    if (a.length != b.length) return false

    a.indices.forall(i => a(i).equals(b(i)))
  }
}

class SyncEnumAutomaticIOArray
(gui: Int, valFunc: () => Array[EnumAutomaticIO], setValFunc: (Array[EnumAutomaticIO]) => Unit)
  extends SyncBase[Array[EnumAutomaticIO]](gui, valFunc, setValFunc, SyncEnumAutomaticIOArray.storageEquals) {
  override def writeNBT(): NBTBase = {
    val tag = new NBTTagCompound
    tag.setInteger(SyncStringArray.SIZE_KEY, value.length)
    value.indices.foreach(i => tag.setInteger(i.toString, value(i).ordinal()))
    tag
  }

  override def handleNBT(nbt: NBTBase): Unit = {
    val array = new Array[EnumAutomaticIO](nbt.asInstanceOf[NBTTagCompound].getInteger(SyncStringArray.SIZE_KEY))
    array.indices.foreach(i => array(i) = EnumAutomaticIO.values()(nbt.asInstanceOf[NBTTagCompound].getInteger(i.toString)))
    value = array
  }
}
