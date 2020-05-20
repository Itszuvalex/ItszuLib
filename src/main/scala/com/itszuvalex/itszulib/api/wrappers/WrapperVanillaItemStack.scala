package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.api.core.{IModule, ItemIdentifier}
import com.itszuvalex.itszulib.implicits.IDImplicits._
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

/**
  * Created by Chris on 4/17/2016.
  */
object WrapperVanillaItemStack {

  def apply(stack: ItemStack): IItemStack = {
    if (stack == null)
      IItemStack.Empty
    else
      new WrapperVanillaItemStack(stack)
  }

  def loadFromNBT(n: NBTTagCompound): IItemStack = nbtDeserializer.apply(n)

  val nbtWriter = new Overridable((c: WrapperVanillaItemStack, nbt: NBTTagCompound) =>
                                    Option(c.toMinecraft).foreach(_.writeToNBT(nbt)))

  val nbtSerializer = new Overridable((c: WrapperVanillaItemStack) => {
    Option(c.toMinecraft).map(_.serializeNBT()).orNull
  })

  val nbtDeserializer = new Overridable((n: NBTTagCompound) => {
    WrapperVanillaItemStack(new ItemStack(n))
  })
}

class WrapperVanillaItemStack(private var stack: ItemStack) extends IItemStack {
  def this(item: Item, amount: Int, damage: Int, nbt: NBTTagCompound) = this(new ItemStack(item, amount, damage, nbt))

  def this(item: Item, amount: Int, damage: Int) = this(item, amount, damage, null)

  def this(item: Item, amount: Int) = this(item, amount, 0)

  def this(item: Item) = this(item, 0)

  def this() = this(null.asInstanceOf[Item])

  override def itemID: Int = item.itemID

  override def amount: Int = toMinecraft.getCount

  override def amount_=(amount: Int): Unit = toMinecraft.setCount(amount)

  override def amountMax: Int = toMinecraft.getMaxStackSize

  override def damage: Int = toMinecraft.getItemDamage

  override def damage_=(dam: Int): Unit = toMinecraft.setItemDamage(dam)

  override def nbt_=(nbt: NBTTagCompound): Unit = toMinecraft.setTagCompound(nbt)

  override def damageMax: Int = item.getMaxDamage(toMinecraft)

  override def serializeNBT(): NBTTagCompound = WrapperVanillaItemStack.nbtSerializer.apply(this)

  override def item: Item = toMinecraft.getItem

  override def nbt: NBTTagCompound = toMinecraft.getTagCompound

  override def copy(): IItemStack = WrapperVanillaItemStack(Option(toMinecraft).map(_.copy()).orNull)

  override def toMinecraft: ItemStack = if (stack != null) stack else ItemStack.EMPTY

  override def writeToNBT(nbt: NBTTagCompound): Unit = WrapperVanillaItemStack.nbtWriter.apply(this, nbt)

  override def isEmpty: Boolean = stack == null || stack.isEmpty

  override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = if (isEmpty) null.asInstanceOf[T] else stack.getCapability(capability, facing)

  override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = !isEmpty && stack.hasCapability(capability, facing)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    stack = new ItemStack(nbt)
  }

  override def identifier: ItemIdentifier = ItemIdentifier(item)

  //TODO: Check once we have Item wrapper/interface, check for it and look for modules from it first before falling back to Capabilities

  override def hasModule(mod: IModule[_], facing: EnumFacing): Boolean = if (mod.hasCapability) hasCapability(mod.capability, facing) else false

  override def getModule[T](mod: IModule[T], facing: EnumFacing): T = if (mod.hasCapability) getCapability(mod.capability, facing) else null.asInstanceOf[T]
}
