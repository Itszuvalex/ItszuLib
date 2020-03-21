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

    override def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = IFluidStack.Empty

    override def canFillFluidType(index: Int, resource: IFluidStack): Boolean = false

    override def canDrainFluidType(index: Int, resource: IFluidStack): Boolean = false

    override def length: Int = 0

    override def apply(idx: Int): IFluidStack = IFluidStack.Empty
  }
}

trait IFluidStorage extends scala.collection.immutable.Seq[IFluidStack] with INBTSerializable[NBTTagCompound] {
  def canFillFluidType(index: Int, resource: IFluidStack): Boolean = resource != null && !resource.isEmpty && canFill(index)

  def canDrainFluidType(index: Int, resource: IFluidStack): Boolean = resource != null && !resource.isEmpty && canDrain(index)

  def canDrain(index: Int): Boolean = true

  def canFill(index: Int): Boolean = true

  /**
    *
    * @param resource
    * @param doFill
    * @return Amount of resource filled
    */
  def fill(resource: IFluidStack, doFill: Boolean): Int

  /**
    *
    * @param resource
    * @param doDrain
    * @return Amount of resource drained
    */
  def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack

  def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack

  def capacity(index: Int): Int = apply(index).amountMax

  override def iterator: Iterator[IFluidStack] = new FluidStorageIterator(this)


}
