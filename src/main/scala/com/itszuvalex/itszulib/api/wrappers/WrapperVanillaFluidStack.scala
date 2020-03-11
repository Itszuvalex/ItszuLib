package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.Overridable
import com.itszuvalex.itszulib.api.core.FluidIdentifier
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.{Fluid, FluidStack}

/**
  * Created by Chris on 7/6/2016.
  */
object WrapperVanillaFluidStack {
  def apply(fluid: FluidStack) = {
    if (fluid == null)
      IFluidStack.Empty
    else new WrapperVanillaFluidStack(fluid)
  }

  def loadFromNBT(n: NBTTagCompound) = nbtDeserializer.apply(n)

  val nbtWriter = new Overridable((c: WrapperVanillaFluidStack, nbt: NBTTagCompound) =>
                                    Option(c.toMinecraft).foreach(_.writeToNBT(nbt)))

  val nbtSerializer = new Overridable((c: WrapperVanillaFluidStack) => {
    val nbt = new NBTTagCompound
    c.toMinecraft match {
      case null => null
      case a =>
        a.writeToNBT(nbt)
        nbt
    }
  })

  val nbtDeserializer = new Overridable((n: NBTTagCompound) => {
    new WrapperVanillaFluidStack(FluidStack.loadFluidStackFromNBT(n))
  }
                                        )

  val nbtSelfModifyingDeserializer = new Overridable((c: WrapperVanillaFluidStack, n: NBTTagCompound) => {
    c.fluidStack = FluidStack.loadFluidStackFromNBT(n)
  })
}

class WrapperVanillaFluidStack(private var fluidStack: FluidStack) extends IFluidStack {

  def this() = this(null)

  override def toMinecraft = fluidStack

  override def fluid: Fluid = toMinecraft.getFluid

  override def amount_=(amount: Int): Unit = toMinecraft.amount = amount

  override def amount: Int = toMinecraft.amount

  override def amountMax = Int.MaxValue

  override def copy(): IFluidStack = WrapperVanillaFluidStack(Option(toMinecraft).map(_.copy()).orNull)

  override def isFluidEqual(o: IFluidStack): Boolean = {
    if (o == null)
      false
    else if (isEmpty && o.isEmpty)
      true
    else if (isEmpty && !o.isEmpty)
      false
    else if (!isEmpty && o.isEmpty)
      false
    else
      toMinecraft.isFluidEqual(o.toMinecraft)
  }

  override def nbt_=(nbt: NBTTagCompound): Unit = toMinecraft.tag = nbt

  override def nbt: NBTTagCompound = toMinecraft.tag

  override def deserializeNBT(nbt: NBTTagCompound): Unit = WrapperVanillaFluidStack.nbtSelfModifyingDeserializer.apply(this, nbt)

  override def serializeNBT(): NBTTagCompound = WrapperVanillaFluidStack.nbtSerializer.apply(this)

  override def writeToNBT(nbt: NBTTagCompound): Unit = WrapperVanillaFluidStack.nbtWriter.apply(this, nbt)

  override def isEmpty: Boolean = fluidStack == null

  override def identifier: FluidIdentifier = FluidIdentifier(fluid)
}
