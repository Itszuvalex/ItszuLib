package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.Overridable
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.{Fluid, FluidStack}

/**
  * Created by Chris on 7/6/2016.
  */
object WrappedFluidStack {
  def loadFromNBT(n: NBTTagCompound) = nbtDeserializer.apply(n)

  val nbtWriter = new Overridable((c: WrappedFluidStack, nbt: NBTTagCompound) =>
                                    c.toMinecraft.foreach(_.writeToNBT(nbt)))

  val nbtSerializer = new Overridable((c: WrappedFluidStack) => {
    val nbt = new NBTTagCompound
    c.toMinecraft match {
      case Some(a) =>
        a.writeToNBT(nbt)
        nbt
      case _ => null
    }
  })

  val nbtDeserializer = new Overridable((n: NBTTagCompound) => {
    new WrappedFluidStack(FluidStack.loadFluidStackFromNBT(n))
  }
                                       )

  val nbtSelfModifyingDeserializer = new Overridable((c: WrappedFluidStack, n: NBTTagCompound) => {
    c.fluidStack = FluidStack.loadFluidStackFromNBT(n)
  })
}

case class WrappedFluidStack(private var fluidStack: FluidStack) extends IFluidStack {


  override def toMinecraft = Option(fluidStack)

  override def fluid: Fluid = toMinecraft.map(_.getFluid).orNull

  override def amount_=(amount: Int): Unit = toMinecraft.foreach(_.amount = amount)

  override def amount: Int = toMinecraft.map(_.amount).getOrElse(0)

  override def copy: IFluidStack = WrappedFluidStack(toMinecraft.map(_.copy()).orNull)

  override def isFluidEqual(o: IFluidStack): Boolean = {
    if (o == null)
      false
    else if (toMinecraft.isEmpty && o.toMinecraft.isEmpty)
      true
    else if (toMinecraft.isEmpty && o.toMinecraft.isDefined)
      false
    else if (toMinecraft.isDefined && o.toMinecraft.isEmpty)
      false
    else
      toMinecraft.get.isFluidEqual(o.toMinecraft.get)
  }

  override def nbt_=(nbt: NBTTagCompound): Unit = toMinecraft.foreach(_.tag = nbt)

  override def nbt: NBTTagCompound = toMinecraft.map(_.tag).orNull

  override def deserializeNBT(nbt: NBTTagCompound): Unit = WrappedFluidStack.nbtSelfModifyingDeserializer.apply(this, nbt)

  override def serializeNBT(): NBTTagCompound = WrappedFluidStack.nbtSerializer.apply(this)

  override def writeToNBT(nbt: NBTTagCompound): Unit = WrappedFluidStack.nbtWriter.apply(this, nbt)
}
