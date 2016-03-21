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
package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.core.traits.tile.TileDescriptionPacket
import com.itszuvalex.itszulib.util.DataUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

abstract class TileEntityBase extends TileEntity with TileDescriptionPacket {
  override def readFromNBT(par1nbtTagCompound: NBTTagCompound) {
    super.readFromNBT(par1nbtTagCompound)
    DataUtils.loadObjectFromNBT(par1nbtTagCompound, this, DataUtils.EnumSaveType.WORLD)
  }

  override def writeToNBT(par1nbtTagCompound: NBTTagCompound) {
    super.writeToNBT(par1nbtTagCompound)
    DataUtils.saveObjectToNBT(par1nbtTagCompound, this, DataUtils.EnumSaveType.WORLD)
  }

  override def updateEntity() {
    super.updateEntity()
    if (!worldObj.isRemote) serverUpdate()
    else clientUpdate()
  }


  /**
    * Gated update call. This will only be called on the server. This should be used instead of updateEntity() for heavy computation, unless the tile absolutely needs to
    * update.
    */
  def serverUpdate() {
  }

  /**
    * Gated update call.  This will only be called on the client.  This should be used instead of updateEntity() for client-side only things like rendering/sounds.
    */
  def clientUpdate() = {

  }

  def getLoc = new Loc4(this)

  def loadInfoFromItemNBT(compound: NBTTagCompound) {
    if (compound == null) {
      return
    }
    DataUtils.loadObjectFromNBT(compound, this, DataUtils.EnumSaveType.ITEM)
  }

  def saveInfoToItemNBT(compound: NBTTagCompound) {
    DataUtils.saveObjectToNBT(compound, this, DataUtils.EnumSaveType.ITEM)
  }

  def onSideActivate(par5EntityPlayer: EntityPlayer, side: Int): Boolean = {
    if (hasGUI) {
      par5EntityPlayer.openGui(getMod, getGuiID, worldObj, xCoord, yCoord, zCoord)
      return true
    }
    false
  }

  def hasGUI = false

  /**
    * @return GuiID, if GUI handler uses ids and not checking instanceof
    */
  def getGuiID = -1

  def getMod: AnyRef

  def canPlayerUse(player: EntityPlayer) = true

  @Deprecated def onInventoryChanged() = setModified()

  def setModified() = if (worldObj != null) worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this)

  def setRenderUpdate() = if (worldObj != null) worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord)

  def setUpdate() = if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord)

  def notifyNeighborsOfChange() = if (worldObj != null) worldObj.func_147453_f(xCoord, yCoord, zCoord, getBlockType)

  def onBlockBreak(): Unit = {}

}
