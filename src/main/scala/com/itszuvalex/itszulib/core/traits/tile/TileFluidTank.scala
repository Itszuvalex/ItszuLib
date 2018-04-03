package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.api.storage.IFluidStorage
import com.itszuvalex.itszulib.core.TileEntityBase
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 11/30/2014.
  */
object TileFluidTank {
  val TANK_NBT = "Tank"
}

trait TileFluidTank extends TileEntityBase {
  var tank = defaultTank

  def defaultTank: IFluidStorage

  override def readFromNBT(compound: NBTTagCompound): Unit = {
    super.readFromNBT(compound)
    tank.deserializeNBT(compound.getCompoundTag(TileFluidTank.TANK_NBT))
  }

  override def writeToNBT(compound: NBTTagCompound): NBTTagCompound = {
    val ret = super.writeToNBT(compound)
    ret.setTag(TileFluidTank.TANK_NBT, tank.serializeNBT())
    ret
  }
}
