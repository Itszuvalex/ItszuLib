package com.itszuvalex.itszulib

import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound

import com.itszuvalex.itszulib.implicits.IDImplicits._

/**
  * Created by Chris on 4/17/2016.
  */
package object wrappers {

  implicit class ItemStackToIItemStack(stack: ItemStack) extends IItemStack {
    override def item: Item = stack.getItem

    override def itemID: Int = stack.getItem.itemID

    override def stackSize: Int = stack.stackSize

    override def stackSize_=(size: Int): Unit = stack.stackSize = size

    override def damage: Int = stack.getItemDamage

    override def damage_=(dam: Int): Unit = stack.setItemDamage(dam)

    override def stackSizeMax: Int = stack.getMaxStackSize

    override def damageMaximum: Int = stack.getMaxDamage

    override def deserializeNBT(nbt: NBTTagCompound): Unit = stack.deserializeNBT(nbt)

    override def serializeNBT(): NBTTagCompound = stack.serializeNBT()

    override def nbt: NBTTagCompound = stack.getTagCompound

    override def nbt_=(nbt: NBTTagCompound): Unit = stack.setTagCompound(nbt)
  }

}
