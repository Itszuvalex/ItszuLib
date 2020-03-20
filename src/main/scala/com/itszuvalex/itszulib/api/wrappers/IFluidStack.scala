package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.api.core.FluidIdentifier
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.fluids.{Fluid, FluidStack}

/**
  * Created by Chris on 7/6/2016.
  */
object IFluidStack {
  val Empty: IFluidStack = new WrapperVanillaFluidStack() {
    override def amount: Int = 0

    override def amount_=(amount: Int): Unit = {}

    override def amountMax: Int = Int.MaxValue

    override def copy(): IFluidStack = IFluidStack.Empty

    override def fluid: Fluid = null

    override def isEmpty: Boolean = true

    override def nbt: NBTTagCompound = null

    override def nbt_=(nbt: NBTTagCompound): Unit = {}

    override def toMinecraft: FluidStack = null
  }

  val nbtLoader = new Overridable((nbt: NBTTagCompound) => {
    val fluidStack = WrapperVanillaFluidStack(null)
    fluidStack.deserializeNBT(nbt)
    fluidStack.asInstanceOf[IFluidStack]
  })

  def createFromNBT(nbt: NBTTagCompound): IFluidStack = nbtLoader.apply(nbt)

  val fluidEquality = new Overridable((a: IFluidStack, b: IFluidStack) => {
    if (a == null)
      b == null
    else if (a.toMinecraft == null)
      b.toMinecraft == null
    else
      a.toMinecraft.isFluidEqual(b.toMinecraft)
  })
}

trait IFluidStack extends INBTSerializable[NBTTagCompound] {

  def fluid: Fluid

  def amount: Int

  def amount_=(amount: Int): Unit

  def amountMax: Int

  def nbt: NBTTagCompound

  def nbt_=(nbt: NBTTagCompound): Unit

  def toMinecraft: FluidStack

  def isEmpty: Boolean

  def copy(): IFluidStack

  def isFluidEqual(o: IFluidStack): Boolean = IFluidStack.fluidEquality.apply(this, o)

  def writeToNBT(nbt: NBTTagCompound): Unit

  def identifier: FluidIdentifier

}
