package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.access.NBTItemCollectionAccess._
import com.itszuvalex.itszulib.implicits.NBTHelpers.NBTAdditions._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

import scala.collection.JavaConversions._

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
object NBTItemCollectionAccess {
  val SIZE_KEY = "Size"
}

/**
  * The given NBT compound has a specific setting.
  * Key: String -> Value: Type
  *
  *
  * Size -> # of ItemStacks stored in this compound: Int
  * 0 -> ItemStack 0: NBTTagCompound
  * .
  * .
  * .
  * (size - 1) -> ItemStack (size -1): NBTTagCompound
  *
  * @param nbt NBT compound storing items.
  */
class NBTItemCollectionAccess(private[access] var nbt: NBTTagCompound, isEmpty: Boolean = false, caching: Boolean = true) extends IItemCollectionAccess {
  if (isEmpty)
    initializeEmptyNBT()

  override def canPlayerAccess(player: EntityPlayer): Boolean = true

  override def length: Int = nbt.Int(SIZE_KEY)

  override def apply(idx: Int): IItemAccess = new NBTItemAccess(this, idx)

  override def copyFromAccess(access: IItemCollectionAccess, copy: Boolean): Unit = {
    nbt.func_150296_c().collect { case i: String => i }.foreach(nbt.removeTag)
    setSize(access.length) //This lets us copy from any access
    super.copyFromAccess(access, copy)
  }

  def setSize(i: Int) = nbt.setInteger(SIZE_KEY, i)

  private[itszuvalex] def updateBackingStore(n: NBTTagCompound, isEmpty: Boolean = false) = {
    nbt = n
    if (isEmpty)
      initializeEmptyNBT()
    onInventoryChanged(-1)
  }

  def initializeEmptyNBT() = {
    nbt.func_150296_c().collect { case i: String => i }.foreach(nbt.removeTag)
    setSize(0)
  }
}
