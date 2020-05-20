package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.api.core.{IModule, INBTObjectSerializer, ItemIdentifier}
import com.itszuvalex.itszulib.api.storage.IStack
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.{Capability, ICapabilitySerializable}

/**
  * Created by Chris on 4/17/2016.
  */
object IItemStack {
  object Empty extends IItemStack {
    override def copy(): IItemStack = IItemStack.Empty

    override def damageMax: Int = 0

    override def damage_=(dam: Int): Unit = {}

    override def damage: Int = 0

    override def isEmpty: Boolean = true

    override def item: Item = null

    override def itemID: Int = 0

    override def nbt: NBTTagCompound = null

    override def nbt_=(nbt: NBTTagCompound): Unit = {}

    override def amount: Int = 0

    override def amount_=(amount: Int): Unit = {}

    override def amountMax: Int = 64

    override def toMinecraft: ItemStack = ItemStack.EMPTY

    override def identifier: ItemIdentifier = ItemIdentifier("minecraft", "air")

    override def writeToNBT(nbt: NBTTagCompound): Unit = {}

    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = false

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = null.asInstanceOf[T]

    override def serializeNBT(): NBTTagCompound = new NBTTagCompound

    override def deserializeNBT(nbt: NBTTagCompound): Unit = {}

    override def hasModule(mod: IModule[_], facing: EnumFacing): Boolean = false

    override def getModule[T](mod: IModule[T], facing: EnumFacing): T = null.asInstanceOf[T]
  }

  def Serializer: INBTObjectSerializer[IItemStack, NBTTagCompound] = OverrideSerializer.getOrElse(VanillaSerializer)

  val VanillaSerializer: INBTObjectSerializer[IItemStack, NBTTagCompound] = new INBTObjectSerializer[IItemStack, NBTTagCompound] {
    override def serialize(obj: IItemStack, nbt: NBTTagCompound): Unit = obj.writeToNBT(nbt)

    override def deserialize(nbt: NBTTagCompound): IItemStack = WrapperVanillaItemStack.nbtDeserializer.apply(nbt)
  }

  var OverrideSerializer: Option[INBTObjectSerializer[IItemStack, NBTTagCompound]] = None

  def createFromNBT(nbt: NBTTagCompound): IItemStack = Serializer.deserialize(nbt)

  val itemStackEquality = new Overridable((a: IItemStack, b: IItemStack) => {
    (a == null && b == null) || !(a == null || b == null) && (ItemStack.areItemsEqual(a.toMinecraft, b.toMinecraft) && ItemStack.areItemStackTagsEqual(a.toMinecraft, b.toMinecraft))
  })
}

trait IItemStack extends IStack[Int] with ICapabilitySerializable[NBTTagCompound] {

  def item: Item

  def itemID: Int

  def damage: Int

  def damage_=(dam: Int): Unit

  def damageMax: Int

  def nbt: NBTTagCompound

  def nbt_=(nbt: NBTTagCompound): Unit

  def hasNbt: Boolean = nbt != null

  def toMinecraft: ItemStack

  def identifier: ItemIdentifier

  def isEmpty: Boolean

  def copy(): IItemStack

  def isItemEqual(o: IItemStack): Boolean = IItemStack.itemStackEquality.apply(this, o)

  def writeToNBT(nbt: NBTTagCompound): Unit

  def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean

  def getCapability[T](capability: Capability[T], facing: EnumFacing): T

  def capabilityOption[T](capability: Capability[T], facing: EnumFacing): Option[T] =
    if (hasCapability(capability, facing)) Option(getCapability(capability, facing)) else None

  def hasModule(mod: IModule[_], facing: EnumFacing): Boolean

  def getModule[T](mod: IModule[T], facing: EnumFacing): T

  def moduleOption[T](mod: IModule[T], facing: EnumFacing): Option[T] =
    if (hasModule(mod, facing)) Option(getModule(mod, facing)) else None
}
