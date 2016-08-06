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
    if (!hasDescription) {
      return null
    }
    val compound: NBTTagCompound = new NBTTagCompound
    saveToDescriptionCompound(compound)
    new SPacketUpdateTileEntity(getPos, getBlockType.getMetaFromState(getWorld.getBlockState(getPos)), compound)
  }

  def saveToDescriptionCompound(compound: NBTTagCompound) {
    DataUtils.saveObjectToNBT(compound, this, DataUtils.EnumSaveType.DESCRIPTION)
  }

  def hasDescription: Boolean

  override def onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
    super.onDataPacket(net, pkt)
    handleDescriptionNBT(pkt.getNbtCompound)
  }

  def handleDescriptionNBT(compound: NBTTagCompound) {
    DataUtils.loadObjectFromNBT(compound, this, DataUtils.EnumSaveType.DESCRIPTION)
  }
}
