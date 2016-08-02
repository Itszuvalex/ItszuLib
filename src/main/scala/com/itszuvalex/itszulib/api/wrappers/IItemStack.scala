package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.Overridable
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

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

    override def item_=(i: Item): Unit = {}

    override def itemID: Int = 0

    override def nbt: NBTTagCompound = null

    override def nbt_=(nbt: NBTTagCompound): Unit = {}

    override def stackSize: Int = 0

    override def stackSize_=(size: Int): Unit = {}

    override def toMinecraft: ItemStack = null
  }

  val nbtLoader = new Overridable((nbt: NBTTagCompound) => {
    WrapperVanillaItemStack.nbtDeserializer.apply(nbt)
  })

  def createFromNBT(nbt: NBTTagCompound) = nbtLoader.apply(nbt)

  val itemStackEquality = new Overridable((a: IItemStack, b: IItemStack) => {
    ItemStack.areItemsEqual(a.toMinecraft, b.toMinecraft) && ItemStack.areItemStackTagsEqual(a.toMinecraft, b.toMinecraft)
  })
}

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

  def toMinecraft: ItemStack

  def isEmpty: Boolean

  def copy(): IItemStack

  def isItemEqual(o: IItemStack): Boolean = IItemStack.itemStackEquality.apply(this, o)

  def writeToNBT(nbt: NBTTagCompound): Unit

}
