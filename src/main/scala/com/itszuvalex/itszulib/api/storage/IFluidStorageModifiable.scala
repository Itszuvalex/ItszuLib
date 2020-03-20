package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.NBTTagCompound

object IFluidStorageModifiable {
  val Empty: IFluidStorageModifiable = new IFluidStorageModifiable {
    override def update(i: Int, s: IFluidStack): Unit = {}

    override def fill(resource: IFluidStack, doFill: Boolean): Int = IFluidStorage.Empty.fill(resource, doFill)

    override def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = IFluidStorage.Empty.drain(resource, doDrain)

    override def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = IFluidStorage.Empty.drainIStack(maxDrain, doDrain)

    override def length: Int = IFluidStorage.Empty.length

    override def apply(idx: Int): IFluidStack = IFluidStorage.Empty.apply(idx)

    override def serializeNBT(): NBTTagCompound = IFluidStorage.Empty.serializeNBT()

    override def deserializeNBT(nbt: NBTTagCompound): Unit = IFluidStorage.Empty.deserializeNBT(nbt)
  }
}

trait IFluidStorageModifiable extends IFluidStorage {

  def update(i: Int, s: IFluidStack): Unit
}
