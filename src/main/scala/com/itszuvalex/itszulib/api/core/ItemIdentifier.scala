package com.itszuvalex.itszulib.api.core

import net.minecraft.item.Item
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.util.INBTSerializable

object ItemIdentifier {
  def apply(nbt: NBTTagCompound): ItemIdentifier = {
    val bi = ItemIdentifier("", "")
    bi.deserializeNBT(nbt)
    bi
  }

  def apply(item: Item): ItemIdentifier =
    ItemIdentifier(
      item.getRegistryName.getResourceDomain,
      item.getRegistryName.getResourcePath)

  val NBT_MOD_ID_KEY  = "m"
  val NBT_ITEM_ID_KEY = "i"
}


case class ItemIdentifier(var modId: String, var itemId: String) extends INBTSerializable[NBTTagCompound] {

  def item: Item = Item.REGISTRY.getObject(resourceLocation)

  def resourceLocation: ResourceLocation = new ResourceLocation(modId, itemId)

  override def serializeNBT(): NBTTagCompound = {
    val ret = new NBTTagCompound
    ret.setString(ItemIdentifier.NBT_MOD_ID_KEY, modId)
    ret.setString(ItemIdentifier.NBT_ITEM_ID_KEY, itemId)
    ret
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    modId = nbt.getString(ItemIdentifier.NBT_ITEM_ID_KEY)
    itemId = nbt.getString(ItemIdentifier.NBT_MOD_ID_KEY)
  }
}
