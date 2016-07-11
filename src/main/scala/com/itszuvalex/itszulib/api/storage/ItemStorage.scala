package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{FloatingItemAccess, IItemAccess}
import com.itszuvalex.itszulib.api.wrappers.IItemStack
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/16.
  */
class ItemStorage(private var item: IItemStack) extends IStorage[ItemStorage, IItemAccess, IItemStack] {

  def this() = this(null)

  override def getAccess: IItemAccess = new FloatingItemAccess(item)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    item = IItemStack.createFromNBT(nbt)
    onChanged()
  }

  override def serializeNBT(): NBTTagCompound = item.serializeNBT()
}
