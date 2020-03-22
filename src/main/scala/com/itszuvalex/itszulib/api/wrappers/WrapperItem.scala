package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.core.IModuleProvider
import net.minecraft.item.Item
import net.minecraft.nbt.NBTTagCompound

class WrapperItem(val item: Item) extends IItem {
  override def toMinecraft: Item = item

  override def initModules(stack: IItemStack, nbt: NBTTagCompound): IModuleProvider = Converter.IModuleProviderFromICapabilityProvider(item.initCapabilities(Converter.ItemStackFromIItemStack(stack), nbt))
}
