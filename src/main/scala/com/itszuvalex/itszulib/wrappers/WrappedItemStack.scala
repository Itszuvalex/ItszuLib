package com.itszuvalex.itszulib.wrappers

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.implicits.IDImplicits._
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 4/17/2016.
  */
object WrappedItemStack {

  val nbtSerializer = new Overridable((c: WrappedItemStack) => {
    c.asMinecraft.serializeNBT()
  })

  val nbtDeserializer = new Overridable((n: NBTTagCompound) => {
    new WrappedItemStack(ItemStack.loadItemStackFromNBT(n))
  }
                                       )

  val nbtSelfModifyingDeserializer = new Overridable((c: WrappedItemStack, n: NBTTagCompound) => {
    c.asMinecraft.readFromNBT(n)
  })
}

case class WrappedItemStack(private val stack: ItemStack) extends IItemStack {
  def this(item: Item, amount: Int, damage: Int, nbt: NBTTagCompound) = this(new ItemStack(item, amount, damage, nbt))

  def this(item: Item, amount: Int, damage: Int) = this(item, amount, damage, null)

  def this(item: Item, amount: Int) = this(item, amount, 0)

  def this(item: Item) = this(item, 0)

  override def itemID: Int = item.itemID

  override def stackSize_=(size: Int): Unit = stack.stackSize = size

  override def damage: Int = stack.getItemDamage

  override def damage_=(dam: Int): Unit = stack.setItemDamage(dam)

  override def stackSize: Int = stack.stackSize

  override def stackSizeMax: Int = stack.stackSizeMax

  override def nbt_=(nbt: NBTTagCompound): Unit = stack.setTagCompound(nbt)

  override def damageMaximum: Int = item.getMaxDamage(asMinecraft)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = WrappedItemStack.nbtSelfModifyingDeserializer.apply(this, nbt)

  override def serializeNBT(): NBTTagCompound = WrappedItemStack.nbtSerializer.apply(this)

  override def item: Item = stack.getItem

  override def nbt: NBTTagCompound = stack.getTagCompound

  override def item_=(i: Item): Unit = stack.setItem(i)

  implicit def asMinecraft: ItemStack = stack

}
