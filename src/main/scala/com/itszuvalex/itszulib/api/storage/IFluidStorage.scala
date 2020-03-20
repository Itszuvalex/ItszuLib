package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
object IFluidStorage {
  val Empty: IFluidStorage = new IFluidStorage {
    override def deserializeNBT(nbt: NBTTagCompound): Unit = {}

    override def serializeNBT(): NBTTagCompound = new NBTTagCompound

    override def fill(resource: IFluidStack, doFill: Boolean): Int = 0

    override def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = IFluidStack.Empty

    override def contents: IFluidStack = IFluidStack.Empty

    override def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = IFluidStack.Empty

    override def canFillFluidType(resource: IFluidStack): Boolean = false

    override def canDrainFluidType(resource: IFluidStack): Boolean = false

    override def capacity: Int = 0
  }
}

trait IFluidStorage extends INBTSerializable[NBTTagCompound] {
  def canFillFluidType(resource: IFluidStack): Boolean = resource != null && resource != IFluidStack.Empty && canFill

  def canDrainFluidType(resource: IFluidStack): Boolean = resource != null && resource != IFluidStack.Empty && canDrain

  def canDrain: Boolean = true

  def canFill: Boolean = true

  def fill(resource: IFluidStack, doFill: Boolean): Int

  def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack

  def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack

  def markDirty(): Unit = {}

  def contents: IFluidStack

  def capacity: Int

  def isEmpty: Boolean = contents == null || contents == IFluidStack.Empty || contents.amount <= 0
}
