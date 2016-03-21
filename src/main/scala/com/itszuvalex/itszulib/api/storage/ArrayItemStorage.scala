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

import com.itszuvalex.itszulib.api.access.{ArrayItemCollectionAccess, IItemCollectionAccess, ItemAccessWrapperFactory, NBTItemCollectionAccess}
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

/**
  *
  */
class ArrayItemStorage(private var array: Array[ItemStack]) extends IItemStorage {
  private val access    = new ArrayItemCollectionAccess(array)
  private val invAccess = ItemAccessWrapperFactory.wrap(access)

  def this(size: Int) = this(new Array[ItemStack](size))

  def this() = this(0)

  override def getAccess: IItemCollectionAccess = access

  override def getInventory: IInventory = invAccess

  /**
    * @return ItemStack[] that backs this inventory class. Modifications to it modify this.
    */
  def getArray: Array[ItemStack] = array

  override def saveToNBT(compound: NBTTagCompound) = {
    new NBTItemCollectionAccess(compound, true).copyFromAccess(access, copy = false)
  }

  override def loadFromNBT(compound: NBTTagCompound) = {
    val nbt = new NBTItemCollectionAccess(compound)
    updateBackingStore(new Array[ItemStack](nbt.length))
    access.copyFromAccess(nbt, copy = false)
  }

  private def updateBackingStore(a: Array[ItemStack]): Unit = {
    array = a
    access.updateBackingStore(array)
  }

}
