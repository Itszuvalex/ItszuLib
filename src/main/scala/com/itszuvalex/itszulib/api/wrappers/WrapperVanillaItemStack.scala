package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.implicits.IDImplicits._
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 4/17/2016.
  */
object WrapperVanillaItemStack {

  def loadFromNBT(n: NBTTagCompound) = nbtDeserializer.apply(n)

  val nbtWriter = new Overridable((c: WrapperVanillaItemStack, nbt: NBTTagCompound) =>
                                    Option(c.toMinecraft).foreach(_.writeToNBT(nbt)))

  val nbtSerializer = new Overridable((c: WrapperVanillaItemStack) => {
    Option(c.toMinecraft).map(_.serializeNBT()).orNull
  })

  val nbtDeserializer = new Overridable((n: NBTTagCompound) => {
    new WrapperVanillaItemStack(ItemStack.loadItemStackFromNBT(n))
  }
                                       )

  val nbtSelfModifyingDeserializer = new Overridable((c: WrapperVanillaItemStack, n: NBTTagCompound) => {
    if (c.toMinecraft != null)
      c.toMinecraft.readFromNBT(n)
  })
}

case class WrapperVanillaItemStack(private val stack: ItemStack) extends IItemStack {
  def this(item: Item, amount: Int, damage: Int, nbt: NBTTagCompound) = this(new ItemStack(item, amount, damage, nbt))

  def this(item: Item, amount: Int, damage: Int) = this(item, amount, damage, null)

  def this(item: Item, amount: Int) = this(item, amount, 0)

  def this(item: Item) = this(item, 0)

  override def itemID: Int = item.itemID

  override def stackSize_=(size: Int): Unit = toMinecraft.stackSize = size

  override def damage: Int = toMinecraft.getItemDamage

  override def damage_=(dam: Int): Unit = toMinecraft.setItemDamage(dam)

  override def stackSize: Int = toMinecraft.stackSize

  override def stackSizeMax: Int = toMinecraft.getMaxStackSize

  override def nbt_=(nbt: NBTTagCompound): Unit = toMinecraft.setTagCompound(nbt)

  override def damageMax: Int = item.getMaxDamage(toMinecraft)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = WrapperVanillaItemStack.nbtSelfModifyingDeserializer.apply(this, nbt)

  override def serializeNBT(): NBTTagCompound = WrapperVanillaItemStack.nbtSerializer.apply(this)

  override def item: Item = toMinecraft.getItem

  override def nbt: NBTTagCompound = toMinecraft.getTagCompound

  override def item_=(i: Item): Unit = toMinecraft.setItem(i)

  override def copy(): IItemStack = WrapperVanillaItemStack(Option(toMinecraft).map(_.copy()).orNull)

  override def toMinecraft: ItemStack = stack

  override def writeToNBT(nbt: NBTTagCompound): Unit = WrapperVanillaItemStack.nbtWriter.apply(this, nbt)

  override def isEmpty: Boolean = stack == null
}
