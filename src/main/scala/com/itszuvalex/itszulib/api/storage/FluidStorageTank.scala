package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.{Converter, IFluidStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidTank

class FluidStorageTank(val capacity: Int) extends IFluidStorage {
  private val tank = new FluidTank(capacity)

  /**
    *
    * @param i Index
    *
    * @return Get IFluidStack contained at this location.  This should never return null, as IFluidStacks track their own emptiness.
    */
  override def apply(i: Int): IFluidStack = Converter.IFluidStackFromFluidStack(tank.getFluid)

  /**
    *
    * @param i Index to update
    * @param s IFluidStack to set
    */
  override def update(i: Int, s: IFluidStack): Unit = {
    tank.setFluid(s.toMinecraft)
  }

  override def length: Int = 1

  override def deserializeNBT(t: NBTTagCompound): Unit = {
    tank.readFromNBT(t)
  }

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    tank.writeToNBT(nbt)
    nbt
  }
}
