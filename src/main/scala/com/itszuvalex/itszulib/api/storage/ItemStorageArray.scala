package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IItemStack
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
class ItemStorageArray(private var storage: Array[IItemStack]) extends IItemStorage {
  storage.indices.filter(storage(_) == null).foreach(storage(_) = IItemStack.Empty)

  def this(size: Int) = this(new Array[IItemStack](size))

  def this() = this(0)

  /**
    *
    * @param i Index
    * @return Get IItemStack contained at this location.  This should never return null, as IItemStacks track their own emptiness.
    */
  override def apply(i: Int): IItemStack = storage(i)

  /**
    *
    * @param i Index to update
    * @param s IItemStack to set
    */
  override def update(i: Int, s: IItemStack): Unit = storage(i) = s

  override def length: Int = storage.length

  override def deserializeNBT(t: NBTTagCompound): Unit = {
    storage.indices.
    filter(i =>
             t.hasKey(i.toString)).
    view.
    foreach(i =>
              storage(i) = IItemStack.createFromNBT(t.getCompoundTag(i.toString)))
  }

  override def serializeNBT(): NBTTagCompound = {
    val ret = new NBTTagCompound
    storage.zipWithIndex.
    filterNot { case (it: IItemStack, i: Int) =>
      it.isEmpty
              }
    .view
    .foreach { case (it: IItemStack, i: Int) =>
      ret.setTag(i.toString, it.serializeNBT())
             }
    ret
  }
}

