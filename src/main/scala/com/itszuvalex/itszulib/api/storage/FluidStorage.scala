package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{FloatingFluidAccess, IFluidAccess}
import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 7/10/2016.
  */
class FluidStorage(private var fluid: IFluidStack) extends IStorage[FluidStorage, IFluidAccess, IFluidStack] {
  override def getAccess: IFluidAccess = new FloatingFluidAccess(fluid)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    fluid = IFluidStack.createFromNBT(nbt)
    onChanged()
  }

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    fluid.writeToNBT(nbt)
    nbt
  }
}
