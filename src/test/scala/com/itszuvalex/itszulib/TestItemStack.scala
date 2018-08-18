package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.wrappers.IItemStack
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

/**
  * Created by Christopher Harris (Itszuvalex) on 7/31/16.
  */
class TestItemStack(var testItem: Int, var testStack: Int, var testDamage: Int) extends IItemStack {

  def this() = this(-1, 0, 0)

  def this(testItem: Int) = this(testItem, 1, 0)

  def this(testItem: Int, testStack: Int) = this(testItem, testStack, 0)

  var testStackMax : Int = 64
  var testDamageMax: Int = 0
  var testNBT            = new NBTTagCompound

  override def item: Item = null

  override def itemID: Int = testItem

  override def stackSize: Int = testStack

  override def stackSize_=(size: Int): Unit = testStack = size

  override def stackSizeMax: Int = testStackMax

  override def damage: Int = testDamage

  override def damage_=(dam: Int): Unit = testDamage = dam

  override def damageMax: Int = testDamageMax

  override def nbt: NBTTagCompound = testNBT

  override def nbt_=(nbt: NBTTagCompound): Unit = testNBT = nbt

  override def toMinecraft: ItemStack = null

  override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = toMinecraft.getCapability(capability, facing)

  override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = toMinecraft.hasCapability(capability, facing)

  override def isEmpty: Boolean = testItem == -1

  override def copy(): IItemStack = {
    val ret = new TestItemStack(testItem, testStack, testDamage)
    ret.nbt = Option(testNBT).map(_.copy()).orNull
    ret
  }

  override def writeToNBT(nbt: NBTTagCompound): Unit = {
    nbt.setInteger("Item", testItem)
    nbt.setInteger("Stack", testStack)
    nbt.setInteger("Damage", testDamage)
    nbt.setTag("NBT", testNBT)
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    testItem = nbt.getInteger("Item")
    testStack = nbt.getInteger("Stack")
    testDamage = nbt.getInteger("Damage")
    testNBT = nbt.getCompoundTag("NBT")
  }

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    writeToNBT(nbt)
    nbt
  }

  override def isItemEqual(o: IItemStack): Boolean = {
    if (o == null) return false
    if (!o.isInstanceOf[TestItemStack]) return false
    val other = o.asInstanceOf[TestItemStack]
    if (other.testItem != testItem) return false
    if (o.damage != damage) return false
    if (o.nbt != nbt) return false
    if (o.nbt != null && !o.nbt.equals(nbt)) return false
    true
  }
}
