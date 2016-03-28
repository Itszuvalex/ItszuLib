/*
 * ******************************************************************************
 *  * Copyright (C) 2013  Christopher Harris (Itszuvalex)
 *  * Itszuvalex@gmail.com
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * as published by the Free Software Foundation; either version 2
 *  * of the License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *  *****************************************************************************
 */
package com.itszuvalex.itszulib.api.storage

import java.util

import com.itszuvalex.itszulib.api.access.{IItemCollectionAccess, ItemAccessWrapperFactory, StorageItemCollectionAccess}
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

/**
  *
  */
class ArrayItemCollectionStorage(private var array: Array[ItemStack]) extends IItemCollectionStorage {
  private val access    = new StorageItemCollectionAccess(this)
  private val invAccess = ItemAccessWrapperFactory.wrap(access)

  def this(size: Int) = this(new Array[ItemStack](size))

  def this() = this(0)

  override def getFullAccess: IItemCollectionAccess = access.synchronized(access)

  override def getInventory: IInventory = access.synchronized(invAccess)

  /**
    * @return ItemStack[] that backs this inventory class. Modifications to it modify this. This is not threadsafe
    */
  def getArray: Array[ItemStack] = array

  override def saveToNBT(compound: NBTTagCompound) =
    access.synchronized {
                          val store = new NBTItemCollectionStorage(compound, true)
                          store.setSize(length, clear = true)
                          store.getFullAccess.copyFromAccess(access, copy = false)
                        }

  override def length: Int = access.synchronized(array.length)

  override def loadFromNBT(compound: NBTTagCompound) =
    access.synchronized {
                          val nbt = new NBTItemCollectionStorage(compound, false)
                          setSize(nbt.length, clear = true)
                          access.copyFromAccess(nbt.getFullAccess, copy = false)
                        }

  /**
    *
    * @param size  Size to attempt to set storage to.
    * @param clear True if should empty current storage.  False to keep items in their locations,
    *              dropping those that are out of bounds (on a size reduction.)
    * @return True if resize successful, false if cannot resize.  False returns should never modify.
    */
  override def setSize(size: Int, clear: Boolean): Boolean = {
    if (clear)
      if (size != length)
        updateBackingStore(new Array[ItemStack](size))
      else {
        indices.foreach(i => this (i) = null)
      }
    else {
      if (size != length)
        updateBackingStore(util.Arrays.copyOf(array, size))
    }
    true
  }

  private def updateBackingStore(a: Array[ItemStack]): Unit = {
    access.synchronized {
                          incrementRevision()
                          array = a
                        }
  }

  override def update(slot: Int, value: ItemStack): Unit = array(slot) = value

  override def apply(slot: Int): ItemStack = array(slot)
}
