package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.util.DataUtils
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity

/**
  * Created by Christopher on 2/20/2015.
  */
trait TileDescriptionPacket extends TileEntity {
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
    DataUtils.saveObjectToNBT(compound, this, DataUtils.EnumSaveType.DESCRIPTION)
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
    DataUtils.loadObjectFromNBT(compound, this, DataUtils.EnumSaveType.DESCRIPTION)
  }
}
