package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.core.IModuleProvider
import com.itszuvalex.itszulib.api.wrappers.{IItem, IItemStack}
import net.minecraft.item.Item
import net.minecraft.nbt.NBTTagCompound

class ItemCore extends Item with IItem {
  override def toMinecraft: Item = this

  override def initModules(stack: IItemStack, nbt: NBTTagCompound): IModuleProvider = null
}
