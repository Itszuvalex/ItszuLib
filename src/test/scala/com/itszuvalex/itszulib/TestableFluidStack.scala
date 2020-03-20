package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.core.FluidIdentifier
import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.{Fluid, FluidStack}

class TestableFluidStack(var fluidId: Int, var fluidAmount: Int) extends IFluidStack {
  def this(id: Int) = this(id, 0)

  var fluidMax                 = Int.MaxValue
  var fluidNBT: NBTTagCompound = null

  override def fluid: Fluid = null

  override def amount: Int = fluidAmount

  override def amount_=(amount: Int): Unit = fluidAmount = amount

  override def amountMax: Int = fluidMax

  override def nbt: NBTTagCompound = fluidNBT

  override def nbt_=(nbt: NBTTagCompound): Unit = fluidNBT = nbt

  override def toMinecraft: FluidStack = null

  override def isEmpty: Boolean = fluidAmount <= 0

  override def copy(): IFluidStack = {
    val copy = new TestableFluidStack(fluidId, fluidAmount)
    copy.fluidMax = fluidMax
    copy.fluidNBT = Option(fluidNBT).map(_.copy()).orNull
    copy
  }

  override def writeToNBT(nbt: NBTTagCompound): Unit = {
    nbt.setInteger("id", fluidId)
    nbt.setInteger("amount", fluidAmount)
    nbt.setInteger("max", fluidMax)
    Option(fluidNBT).foreach(n => nbt.setTag("nbt", n))
  }

  override def identifier: FluidIdentifier = FluidIdentifier("TestFluid")

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    writeToNBT(nbt)
    nbt
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    fluidId = nbt.getInteger("id")
    fluidAmount = nbt.getInteger("amount")
    fluidMax = nbt.getInteger("max")
    fluidNBT = if (nbt.hasKey("nbt")) nbt.getCompoundTag("nbt") else null
  }
}
