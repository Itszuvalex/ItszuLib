package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{FloatingItemAccess, IItemAccess}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/16.
  */
class ItemStackStorage(private var item: ItemStack) extends IStorage[ItemStackStorage, IItemAccess, ItemStack] {

  def this() = this(null)

  override def getAccess: IItemAccess = new FloatingItemAccess(item)

}
