package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

/**
  * Created by Chris on 4/17/2016.
  */
trait IItemStack extends INBTSerializable[NBTTagCompound] {

  def item: Item

  def item_=(i: Item): Unit

  def itemID: Int

  def stackSize: Int

  def stackSize_=(size: Int): Unit

  def stackSizeMax: Int

  def damage: Int

  def damage_=(dam: Int): Unit

  def damageMax: Int

  def nbt: NBTTagCompound

  def nbt_=(nbt: NBTTagCompound): Unit

  def canConvertToMinecraft: Boolean

  def toMinecraft: Option[ItemStack]

  def copy: IItemStack

  def isItemEqual(o: IItemStack) : Boolean

}
