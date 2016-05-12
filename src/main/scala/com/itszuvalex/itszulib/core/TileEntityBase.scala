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
import net.minecraft.client.renderer.texture.ITickable
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing

abstract class TileEntityBase extends TileEntity with TileDescriptionPacket with ITickable {
  override def readFromNBT(par1nbtTagCompound: NBTTagCompound) {
    super.readFromNBT(par1nbtTagCompound)
    DataUtils.loadObjectFromNBT(par1nbtTagCompound, this, DataUtils.EnumSaveType.WORLD)
  }

  override def writeToNBT(par1nbtTagCompound: NBTTagCompound) {
    super.writeToNBT(par1nbtTagCompound)
    DataUtils.saveObjectToNBT(par1nbtTagCompound, this, DataUtils.EnumSaveType.WORLD)
  }

  override def tick(): Unit = {
    if (!getWorld.isRemote) serverUpdate()
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

  def onSideActivate(par5EntityPlayer: EntityPlayer, side: EnumFacing): Boolean = {
    if (hasGUI) {
      par5EntityPlayer.openGui(getMod, getGuiID, getWorld, getPos.getX, getPos.getY, getPos.getZ)
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

  def setModified() = if (getWorld != null) getWorld.markChunkDirty(getPos, this)

  def setRenderUpdate() = if (getWorld != null) getWorld.markBlockRangeForRenderUpdate(getPos.getX, getPos.getY, getPos.getZ, getPos.getX, getPos.getY, getPos.getZ)

  def setUpdate() = if (getWorld != null) getWorld.markBlockForUpdate(getPos)

  def notifyNeighborsOfChange() = if (getWorld != null) getWorld.notifyNeighborsOfStateChange(getPos, getBlockType)

  def onBlockBreak(): Unit = {}

}
