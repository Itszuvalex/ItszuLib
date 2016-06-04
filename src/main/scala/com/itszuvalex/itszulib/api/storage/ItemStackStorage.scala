package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{FloatingItemAccess, IItemAccess}
import com.itszuvalex.itszulib.api.wrappers.{IItemStack, WrappedItemStack}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/16.
  */
class ItemStackStorage(private var item: IItemStack) extends IStorage[ItemStackStorage, IItemAccess, IItemStack] {

  def this() = this(null)

  override def getAccess: IItemAccess = new FloatingItemAccess(item)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    //TODO: ItemStackWrapper
    item = WrappedItemStack(ItemStack.loadItemStackFromNBT(nbt))
    // TODO: ^ This should be embedded as part of load from nbt.
    // Basically look for special nbt value for what type of ItemStack wrapper it is.  If we don't see this,
    // then it was serialized by vanilla code / or my earlier code.  Wrap it now.  Make sure to serialize everything vanilla expects to see
    // so that way we can at least hopefully try and interop somewhat.
    // Note this may break on different storage options, such as large item stacks (count > 64 capable)
  }

  override def serializeNBT(): NBTTagCompound = {
    //TODO: ItemStackWrapper
    item.serializeNBT()
  }
}
