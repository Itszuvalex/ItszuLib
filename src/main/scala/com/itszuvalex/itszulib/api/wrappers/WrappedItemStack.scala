package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.implicits.IDImplicits._
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 4/17/2016.
  */
object WrappedItemStack {

  def loadFromNBT(n: NBTTagCompound) = nbtDeserializer.apply(n)

  val nbtWriter = new Overridable((c: WrappedItemStack, nbt: NBTTagCompound) =>
                                    c.toMinecraft.foreach(_.writeToNBT(nbt)))

  val nbtSerializer = new Overridable((c: WrappedItemStack) => {
    c.toMinecraft.map(_.serializeNBT()).orNull
  })

  val nbtDeserializer = new Overridable((n: NBTTagCompound) => {
    new WrappedItemStack(ItemStack.loadItemStackFromNBT(n))
  }
                                       )

  val nbtSelfModifyingDeserializer = new Overridable((c: WrappedItemStack, n: NBTTagCompound) => {
    if (c.asMinecraft != null)
      c.asMinecraft.readFromNBT(n)
  })
}

case class WrappedItemStack(private val stack: ItemStack) extends IItemStack {
  def this(item: Item, amount: Int, damage: Int, nbt: NBTTagCompound) = this(new ItemStack(item, amount, damage, nbt))

  def this(item: Item, amount: Int, damage: Int) = this(item, amount, damage, null)

  def this(item: Item, amount: Int) = this(item, amount, 0)

  def this(item: Item) = this(item, 0)

  override def itemID: Int = item.itemID

  override def stackSize_=(size: Int): Unit = asMinecraft.stackSize = size

  override def damage: Int = asMinecraft.getItemDamage

  override def damage_=(dam: Int): Unit = asMinecraft.setItemDamage(dam)

  override def stackSize: Int = asMinecraft.stackSize

  override def stackSizeMax: Int = asMinecraft.getMaxStackSize

  override def nbt_=(nbt: NBTTagCompound): Unit = asMinecraft.setTagCompound(nbt)

  override def damageMax: Int = item.getMaxDamage(asMinecraft)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = WrappedItemStack.nbtSelfModifyingDeserializer.apply(this, nbt)

  override def serializeNBT(): NBTTagCompound = WrappedItemStack.nbtSerializer.apply(this)

  override def item: Item = asMinecraft.getItem

  override def nbt: NBTTagCompound = asMinecraft.getTagCompound

  override def item_=(i: Item): Unit = asMinecraft.setItem(i)

  implicit def asMinecraft: ItemStack = stack

  override def copy: IItemStack = WrappedItemStack(asMinecraft.copy())

  override def isItemEqual(o: IItemStack): Boolean = o != null && item == o.item && o.damage == damage

  override def toMinecraft: Option[ItemStack] = Option(stack)

  override def writeToNBT(nbt: NBTTagCompound): Unit = WrappedItemStack.nbtWriter.apply(this, nbt)
}
