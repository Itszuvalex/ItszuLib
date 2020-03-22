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
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{EnumFacing, ITickable}
import net.minecraftforge.common.capabilities.Capability

@Deprecated
abstract class TileEntityBase extends TileEntity with ITickable {
  def update(): Unit = {
    if (!getWorld.isRemote) serverUpdate()
    else clientUpdate()
  }

  override def getUpdatePacket: SPacketUpdateTileEntity = {
    if (!hasDescription) null
    else new SPacketUpdateTileEntity(getPos, getBlockType.getMetaFromState(getWorld.getBlockState(getPos)), getUpdateTag)
  }

  override def getUpdateTag: NBTTagCompound = {
    val compound = super.getUpdateTag
    saveToDescriptionCompound(compound)
    compound
  }

  def saveToDescriptionCompound(compound: NBTTagCompound) {
  }

  def hasDescription: Boolean

  override def onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
    super.onDataPacket(net, pkt)
    handleDescriptionNBT(pkt.getNbtCompound)
  }

  override def handleUpdateTag(tag: NBTTagCompound): Unit = {
    handleDescriptionNBT(tag)
  }

  def handleDescriptionNBT(compound: NBTTagCompound) {
  }

  /**
    * Gated update call. This will only be called on the server. This should be used instead of updateEntity() for heavy computation, unless the tile absolutely needs to
    * update.
    */
  def serverUpdate(): Unit = {
  }

  /**
    * Gated update call.  This will only be called on the client.  This should be used instead of updateEntity() for client-side only things like rendering/sounds.
    */
  def clientUpdate(): Unit = {

  }

  def getLoc = new Loc4(this)

  def loadInfoFromItemNBT(compound: NBTTagCompound) {
  }

  def saveInfoToItemNBT(compound: NBTTagCompound) {
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

  @Deprecated def onInventoryChanged(): Unit = setModified()

  def setModified(): Unit = if (getWorld != null) getWorld.markChunkDirty(getPos, this)

  def setRenderUpdate(): Unit = if (getWorld != null) getWorld.markBlockRangeForRenderUpdate(getPos.getX, getPos.getY, getPos.getZ, getPos.getX, getPos.getY, getPos.getZ)

  def setUpdate(): Unit = if (getWorld != null) {
    getWorld.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
  }

  def notifyNeighborsOfChange(): Unit = if (getWorld != null) getWorld.notifyNeighborsOfStateChange(getPos, getBlockType, true)

  def onBlockBreak(): Unit = {}

  def capabilityOption[T](capability: Capability[T], enumFacing: EnumFacing): Option[T] =
    if (hasCapability(capability, enumFacing)) Some(getCapability(capability, enumFacing)) else None
}
