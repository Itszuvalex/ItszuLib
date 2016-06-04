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
import com.itszuvalex.itszulib.api.wrappers.{IItemStack, WrappedItemStack}
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

/**
  *
  */
class ArrayItemCollectionStorage(private var array: Array[IItemStack]) extends IItemCollectionStorage {
  private val access    = new StorageItemCollectionAccess(this)
  private val invAccess = ItemAccessWrapperFactory.wrap(access)

  def this(size: Int) = this(new Array[IItemStack](size))

  def this() = this(0)

  def this(ar: Array[ItemStack]) = this(ar.map(WrappedItemStack(_).asInstanceOf[IItemStack]))

  override def getFullAccess: IItemCollectionAccess = access.synchronized(access)

  override def getInventory: IInventory = access.synchronized(invAccess)

  /**
    * @return ItemStack[] that backs this inventory class. Modifications to it modify this. This is not threadsafe
    */
  def getArray: Array[IItemStack] = array

  override def serializeNBT(): NBTTagCompound =
    access.synchronized {
                          val compound = new NBTTagCompound
                          val store = new NBTItemCollectionStorage(compound, true)
                          store.setSize(length, clear = true)
                          store.getFullAccess.copyFromAccess(access, copy = false)
                          compound
                        }

  override def deserializeNBT(compound: NBTTagCompound): Unit =
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
        updateBackingStore(new Array[IItemStack](size))
      else {
        indices.foreach(i => this (i) = null)
      }
    else {
      if (size != length)
        updateBackingStore(util.Arrays.copyOf(array, size))
    }
    true
  }

  override def length: Int = access.synchronized(array.length)

  private def updateBackingStore(a: Array[IItemStack]): Unit = {
    access.synchronized {
                          incrementRevision()
                          array = a
                        }
  }

  override def update(slot: Int, value: IItemStack): Unit = array(slot) = value

  override def apply(slot: Int): IItemStack = array(slot)
}
