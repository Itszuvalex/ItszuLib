package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IItemStack
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
class ItemStorageNBT(private var nbt: NBTTagCompound, size: Int) extends IItemStorage {
  /**
    *
    * @param i Index
    *
    * @return Get IItemStack contained at this location.  This should never return null, as IItemStacks track their own emptiness.
    */
  override def apply(i: Int): IItemStack = IItemStack.createFromNBT(nbt.getCompoundTag(i.toString))

  /**
    *
    * @param i Index to update
    * @param s IItemStack to set
    */
  override def update(i: Int, s: IItemStack): Unit = nbt.setTag(i.toString, s.serializeNBT())

  override def length: Int = size
}

