package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IItemCollectionAccess, ItemAccessWrapperFactory, Revisioned, StorageItemCollectionAccess}
import com.itszuvalex.itszulib.api.core.NBTSerializable
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

object IItemStorage {

  class ItemStorageIterator(private val storage: IItemStorage) extends Iterator[Option[ItemStack]] {
    private var index = 0

    override def hasNext: Boolean = index < storage.getSize

    override def next(): Option[ItemStack] = {
      val ret = storage.getItemStack(index)
      index += 1
      ret
    }
  }

}

/**
  * Created by Christopher Harris (Itszuvalex) on 3/13/2016.
  */
trait IItemStorage extends NBTSerializable with Revisioned {

  def getAccess: IItemCollectionAccess = new StorageItemCollectionAccess(this)

  def getInventory: IInventory = ItemAccessWrapperFactory.wrap(getAccess)

  def getSize: Int

  /**
    *
    * @param size  Size to attempt to set storage to.
    * @param clear True if should empty current storage.  False to keep items in their locations,
    *              dropping those that are out of bounds (on a size reduction.)
    * @return True if resize successful, false if cannot resize.  False returns should never modify.
    */
  def setSize(size: Int, clear: Boolean): Boolean

  def getItemStack(slot: Int): Option[ItemStack]

  def setItemStack(slot: Int, item: ItemStack): Unit

  def iterator: Iterator[Option[ItemStack]] = new IItemStorage.ItemStorageIterator(this)

}
