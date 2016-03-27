package com.itszuvalex.itszulib.api.access

import net.minecraft.item.{Item, ItemStack}

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
trait IItemAccess extends IAccess[IItemAccess, ItemStack] {

  def getItem: Option[Item] = get.map(_.getItem)

  /**
    *
    * @param amount Amount to increase ItemStack stacksize by.  Must be >= 0
    * @return Math.min(amount, MaxStorage - CurrentStorage) -> Amount of amount added to the ItemStack.
    */
  def increment(amount: Int): Int = get.map { i =>
    if (amount < 0) return 0

    val inc = Math.min(amount, room.get)
    i.stackSize += inc
    set(i)
    inc
                                            }.getOrElse(0)

  def room: Option[Int] = maxStorage.map(_ - currentStorage.get)

  /**
    *
    * @return Usually ItemStack.MaxStackSize, but could be different for different storages.
    */
  def maxStorage: Option[Int] = get.map(i => Math.min(i.getMaxStackSize, 128)) //128 because ItemStack's deserializer only loads a Byte, and it's signed

  def damage: Option[Int] = get.map(_.getItemDamage)

  def maxDamage: Option[Int] = get.map(_.getMaxDamage)

  /**
    * Call when this changes backing item.
    */
  def onChanged(): Unit = {}

  /**
    *
    * @param amount Amount to remove from this storage and transfer to a new one.
    * @return New item access
    */
  def split(amount: Int): IItemAccess = amount match {
    case invalid if invalid <= 0 => new FloatingItemAccess(null)
    case _ => new FloatingItemAccess(
                                      get.map { i =>
                                        val item = i.copy()
                                        item.stackSize = decrement(amount)
                                        if (item.stackSize > 0)
                                          item
                                        else null
                                              }.orNull
                                    )
  }

  /**
    *
    * @param amount Amount to decrease ItemStack stacksize by.  Must be > 0
    * @return Math.min(amount, CurrentStorage) -> Amount of amount removed from the ItemStack.  Clears ItemStack if Math.min(amount, CurrentStorage) == CurrentStorage
    */
  def decrement(amount: Int): Int = get.map { i =>
    if (amount < 0) return 0

    val dec = Math.min(amount, currentStorage.get)
    i.stackSize -= dec
    if (i.stackSize <= 0)
      clear()
    else
      set(i)
    dec
                                            }.getOrElse(0)

  /**
    *
    * @return Usually ItemStack.StackSize, but could be different for different storages.
    */
  def currentStorage: Option[Int] = get.map(_.stackSize)

  /**
    * Remove Item and metadata from this access.
    */
  def clear(): Unit = {
    set(null)
  }

  /**
    * Copies all required info from another IItemAccess
    *
    * @param other
    */
  def copyFromAccess(other: IItemAccess, copyStack: Boolean = true): Unit =
    set(other.get match {
          case Some(i) => if (copyStack) i.copy() else i
          case None => null
        })

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  def isValid: Boolean = true

}
