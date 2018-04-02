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

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    super.deserializeNBT(nbt)
    tank.deserializeNBT(nbt.getCompoundTag(TileFluidTank.TANK_NBT))
  }

  override def serializeNBT(): NBTTagCompound = {
    val ret = super.serializeNBT()
    ret.setTag(TileFluidTank.TANK_NBT, tank.serializeNBT())
    ret
  }
}
