package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.api.storage.IFluidStorage
import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.{NBTBase, NBTTagCompound}

class SyncIFluidStorage(val storage: IFluidStorage, gui: Int, sync: () => IFluidStorage, write: (IFluidStorage) => Unit) extends SyncBase[IFluidStorage](
  gui, sync, write,
  (a, b) => {
    (a == null && a == b) ||
    (a != null && b != null) &&
    a.contents == b.contents ||
    (a.contents.isFluidEqual(b.contents) && a.contents.amount == b.contents.amount
     && a.capacity == b.capacity)
  }
  ) {
  cachedValue = IFluidStorage.Empty

  override def cache(a: IFluidStorage): IFluidStorage = {
    new IFluidStorage {
      override def deserializeNBT(nbt: NBTTagCompound): Unit = {}

      override def serializeNBT(): NBTTagCompound = new NBTTagCompound

      override def fill(resource: IFluidStack, doFill: Boolean): Int = 0

      override def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = IFluidStack.Empty

      override def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = IFluidStack.Empty

      override def contents: IFluidStack = a.contents.copy()

      override def capacity: Int = a.capacity
    }
  }

  override def writeNBT(): NBTBase = storage.serializeNBT()

  override def handleNBT(nbt: NBTBase): Unit = {
    storage.deserializeNBT(nbt.asInstanceOf[NBTTagCompound])
    value_=(storage)
  }
}
