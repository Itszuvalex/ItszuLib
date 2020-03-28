package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.core.FluidIdentifier
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.{Fluid, FluidStack, IFluidTank}

class WrapperForgeFluidTank(private val tank: IFluidTank) extends IFluidStack {
  override def fluid: Fluid = Option(tank.getFluid).map(_.getFluid).orNull

  override def amount: Int = tank.getFluidAmount

  override def amount_=(amount: Int): Unit = Option(tank.getFluid).foreach(_.amount += amount)

  override def amountMax: Int = tank.getCapacity

  override def nbt: NBTTagCompound = Option(tank.getFluid.tag).getOrElse(new NBTTagCompound)

  override def nbt_=(nbt: NBTTagCompound): Unit = Option(tank.getFluid).foreach(_.tag = nbt)

  override def toMinecraft: FluidStack = tank.getFluid

  override def isEmpty: Boolean = tank.getFluid == null || tank.getFluidAmount == 0

  override def copy(): IFluidStack = null

  override def writeToNBT(nbt: NBTTagCompound): Unit = {}

  override def serializeNBT(): NBTTagCompound = new NBTTagCompound

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {}

  override def identifier: FluidIdentifier = Option(tank.getFluid).map(f => FluidIdentifier(f.getFluid)).orNull
}
