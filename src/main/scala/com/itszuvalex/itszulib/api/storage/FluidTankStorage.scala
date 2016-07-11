package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{FloatingFluidTankAccess, IFluidTankAccess}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidTank

/**
  * Created by Chris on 7/10/2016.
  */
class FluidTankStorage(private var tank: FluidTank) extends IStorage[FluidTankStorage, IFluidTankAccess, FluidTank] {
  override def getAccess: IFluidTankAccess = new FloatingFluidTankAccess(tank)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    tank = new FluidTank(nbt.getInteger("capacity"))
    tank.readFromNBT(nbt)
    onChanged()
  }

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    tank.writeToNBT(nbt)
    nbt.setInteger("capacity", tank.getCapacity)
    nbt
  }
}
