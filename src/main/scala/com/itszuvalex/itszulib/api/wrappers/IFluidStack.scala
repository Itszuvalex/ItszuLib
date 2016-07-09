package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.Overridable
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.fluids.{Fluid, FluidStack}

/**
  * Created by Chris on 7/6/2016.
  */
object IFluidStack {
  val nbtLoader = new Overridable((nbt: NBTTagCompound) => {
    val fluidStack = WrappedFluidStack(null)
    fluidStack.deserializeNBT(nbt)
    fluidStack.asInstanceOf[IFluidStack]
  })

  def createFromNBT(nbt: NBTTagCompound): IFluidStack = nbtLoader.apply(nbt)
}

trait IFluidStack extends INBTSerializable[NBTTagCompound] {

  def fluid: Fluid

  def amount: Int

  def amount_=(amount: Int): Unit

  def nbt: NBTTagCompound

  def nbt_=(nbt: NBTTagCompound): Unit

  def toMinecraft: Option[FluidStack]

  def copy(): IFluidStack

  def isFluidEqual(o: IFluidStack): Boolean

  def writeToNBT(nbt: NBTTagCompound): Unit

}
