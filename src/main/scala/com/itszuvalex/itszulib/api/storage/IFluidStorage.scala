package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.{IFluidStack, WrapperVanillaFluidStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.{IFluidHandler, IFluidTankProperties}

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
object IFluidStorage {
  val Empty: IFluidStorage = new IFluidStorage {
    override def deserializeNBT(nbt: NBTTagCompound): Unit = {}

    override def serializeNBT(): NBTTagCompound = new NBTTagCompound

    override def fill(resource: FluidStack, doFill: Boolean): Int = 0

    override def drain(resource: FluidStack, doDrain: Boolean): FluidStack = null

    override def drain(maxDrain: Int, doDrain: Boolean): FluidStack = null

    override def getTankProperties: Array[IFluidTankProperties] = Array()
  }
}

trait IFluidStorage extends IFluidHandler with INBTSerializable[NBTTagCompound] {
  def fill(resource: IFluidStack, doFill: Boolean): Int = fill(resource.toMinecraft, doFill)

  def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = WrapperVanillaFluidStack(drain(resource.toMinecraft, doDrain))

  def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = WrapperVanillaFluidStack(drain(maxDrain, doDrain))

  def getStorageProperties: Array[IFluidStorageProperties] = getTankProperties.map(WrapperFluidStorageProperties)
}
