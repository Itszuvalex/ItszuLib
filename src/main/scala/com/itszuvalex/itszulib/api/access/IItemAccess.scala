package com.itszuvalex.itszulib.api.access

import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
trait IItemAccess {
  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  def getItemStack: Option[ItemStack]

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  def setItemStack(stack: ItemStack): Unit

  def isEmpty: Boolean = getItemStack.isEmpty

  def isDefined: Boolean = getItemStack.isDefined

  def matches(matcher: (ItemStack) => Boolean) = matcher != null && matcher(getItemStack.orNull)

  def isItemValid(stack: ItemStack): Boolean = true

  /**
    *
    * @param amount Amount to increase ItemStack stacksize by.  Must be >= 0
    * @return Math.min(amount, MaxStorage - CurrentStorage) -> Amount of amount added to the ItemStack.
    */
  def increment(amount: Int): Int = getItemStack.map { i =>
    if (amount < 0) return 0

    val inc = Math.min(amount, room.get)
    i.stackSize += inc
    onChanged()
    inc
                                                     }.getOrElse(0)

  def room: Option[Int] = maxStorage.map(_ - currentStorage.get)

  /**
    *
    * @return Usually ItemStack.MaxStackSize, but could be different for different storages.
    */
  def maxStorage: Option[Int] = getItemStack.map(i => Math.min(i.getMaxStackSize, 128)) //128 because ItemStack's deserializer only loads a Byte, and it's signed

  def damage: Option[Int] = getItemStack.map(_.getItemDamage)

  def maxDamage: Option[Int] = getItemStack.map(_.getMaxDamage)

  /**
    *
    * @param amount Amount to decrease ItemStack stacksize by.  Must be > 0
    * @return Math.min(amount, CurrentStorage) -> Amount of amount removed from the ItemStack.  Clears ItemStack if Math.min(amount, CurrentStorage) == CurrentStorage
    */
  def decrement(amount: Int): Int = getItemStack.map { i =>
    if (amount < 0) return 0

    val dec = Math.min(amount, currentStorage.get)
    i.stackSize -= dec
    if (i.stackSize <= 0)
      clear()
    onChanged()
    dec
                                                     }.getOrElse(0)

  /**
    *
    * @return Usually ItemStack.StackSize, but could be different for different storages.
    */
  def currentStorage: Option[Int] = getItemStack.map(_.stackSize)

  /**
    * Call when this changes backing item.
    */
  def onChanged(): Unit = {}

  /**
    * Remove Item and metadata from this access.
    */
  def clear(): Unit = {
    setItemStack(null)
  }

  /**
    *
    * @param amount Amount to remove from this storage and transfer to a new one.
    * @return New item access
    */
  def split(amount: Int): IItemAccess

  /**
    * Copies all required info from another IItemAccess
    *
    * @param other
    */
  def copyFromAccess(other: IItemAccess, copyStack: Boolean = true): Unit =
    setItemStack(other.getItemStack match {
                   case Some(i) => if (copyStack) i.copy() else i
                   case None => null
                 })

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  def isValid: Boolean

}
