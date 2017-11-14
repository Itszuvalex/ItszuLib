package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.Overridable
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.{Capability, ICapabilitySerializable}

/**
  * Created by Chris on 4/17/2016.
  */
object IItemStack {
  val Empty: IItemStack = new WrapperVanillaItemStack() {
    override def copy(): IItemStack = IItemStack.Empty

    override def damageMax: Int = 0

    override def damage_=(dam: Int): Unit = {}

    override def damage: Int = 0

    override def isEmpty: Boolean = true

    override def item = null

    override def itemID: Int = 0

    override def nbt: NBTTagCompound = null

    override def nbt_=(nbt: NBTTagCompound): Unit = {}

    override def stackSize: Int = 0

    override def stackSize_=(size: Int): Unit = {}

    override def stackSizeMax: Int = 64

    override def toMinecraft: ItemStack = ItemStack.EMPTY
  }

  val nbtLoader = new Overridable((nbt: NBTTagCompound) => {
    WrapperVanillaItemStack.nbtDeserializer.apply(nbt)
  })

  def createFromNBT(nbt: NBTTagCompound): IItemStack = nbtLoader.apply(nbt)

  val itemStackEquality = new Overridable((a: IItemStack, b: IItemStack) => {
    (a == null && b == null) || !(a == null || b == null) && (ItemStack.areItemsEqual(a.toMinecraft, b.toMinecraft) && ItemStack.areItemStackTagsEqual(a.toMinecraft, b.toMinecraft))
  })
}

trait IItemStack extends ICapabilitySerializable[NBTTagCompound] {

  def item: Item

  def itemID: Int

  def stackSize: Int

  def stackSize_=(size: Int): Unit

  def stackSizeMax: Int

  def damage: Int

  def damage_=(dam: Int): Unit

  def damageMax: Int

  def nbt: NBTTagCompound

  def nbt_=(nbt: NBTTagCompound): Unit

  def hasNbt: Boolean = nbt != null

  def toMinecraft: ItemStack

  def isEmpty: Boolean

  def room: Int = stackSizeMax - stackSize

  def copy(): IItemStack

  def isItemEqual(o: IItemStack): Boolean = IItemStack.itemStackEquality.apply(this, o)

  def writeToNBT(nbt: NBTTagCompound): Unit

  def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean

  def getCapability[T](capability: Capability[T], facing: EnumFacing): T

  def capabilityOption[T](capability: Capability[T], facing: EnumFacing): Option[T] =
    if (!isEmpty && hasCapability(capability, facing)) Some(getCapability(capability, facing)) else None
}
