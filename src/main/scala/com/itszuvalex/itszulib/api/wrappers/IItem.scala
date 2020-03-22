package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.core.IModuleProvider
import net.minecraft.item.Item
import net.minecraft.nbt.NBTTagCompound

trait IItem {
  def toMinecraft: Item

  def initModules(stack: IItemStack, nbt: NBTTagCompound): IModuleProvider

}
