package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.util.DataUtils
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.network.{NetworkManager, Packet}
import net.minecraft.tileentity.TileEntity

/**
  * Created by Christopher on 2/20/2015.
  */
trait TileDescriptionPacket extends TileEntity {
  override def getDescriptionPacket: Packet = {
    if (!hasDescription) {
      return null
    }
    val compound: NBTTagCompound = new NBTTagCompound
    saveToDescriptionCompound(compound)
    new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, compound)
  }

  def saveToDescriptionCompound(compound: NBTTagCompound) {
    DataUtils.saveObjectToNBT(compound, this, DataUtils.EnumSaveType.DESCRIPTION)
  }

  def hasDescription: Boolean

  override def onDataPacket(net: NetworkManager, pkt: S35PacketUpdateTileEntity) {
    super.onDataPacket(net, pkt)
    handleDescriptionNBT(pkt.func_148857_g)
  }

  def handleDescriptionNBT(compound: NBTTagCompound) {
    DataUtils.loadObjectFromNBT(compound, this, DataUtils.EnumSaveType.DESCRIPTION)
  }
}
