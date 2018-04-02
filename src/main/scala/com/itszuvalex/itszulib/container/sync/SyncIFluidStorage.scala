package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.api.storage.IFluidStorage
import net.minecraft.nbt.{NBTBase, NBTTagCompound}
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidTankProperties

class SyncIFluidStorage(val storage: IFluidStorage, gui: Int, sync: () => IFluidStorage, write: (IFluidStorage) => Unit) extends SyncBase[IFluidStorage](gui, sync, write,
  (a, b) => {
    (a == null && a == b) ||
      (a != null && b != null) &&
        (a.getStorageProperties.length == b.getStorageProperties.length) &&
        a.getStorageProperties.zipWithIndex.forall { pair =>
          val pair1Stack = pair._1.getContents
          val pair2Stack = b.getStorageProperties(pair._2).getContents
          pair1Stack == pair2Stack && (pair1Stack == null || pair1Stack.amount == pair2Stack.amount)
        }
  }) {
  cachedValue = IFluidStorage.Empty

  override def cache(a: IFluidStorage): IFluidStorage = {
    val array = new Array[IFluidTankProperties](a.getStorageProperties.length)
    array.indices.foreach { i =>
      val stack = Option(a.getStorageProperties(i).getContents).map(_.copy()).orNull
      array(i) = new IFluidTankProperties {
        override def canDrainFluidType(fluidStack: FluidStack): Boolean = a.getStorageProperties(i).canDrainFluidType(fluidStack)

        override def canFill: Boolean = a.getStorageProperties(i).canFill

        override def canDrain: Boolean = a.getStorageProperties(i).canDrain

        override def canFillFluidType(fluidStack: FluidStack): Boolean = a.getStorageProperties(i).canFillFluidType(fluidStack)

        override def getContents: FluidStack = stack

        override def getCapacity: Int = a.getStorageProperties(i).getCapacity
      }
    }
    new IFluidStorage {
      override def deserializeNBT(nbt: NBTTagCompound): Unit = {}

      override def serializeNBT(): NBTTagCompound = new NBTTagCompound

      override def fill(resource: FluidStack, doFill: Boolean): Int = 0

      override def drain(resource: FluidStack, doDrain: Boolean): FluidStack = null

      override def drain(maxDrain: Int, doDrain: Boolean): FluidStack = null

      override def getTankProperties: Array[IFluidTankProperties] = array
    }
  }

  override def writeNBT(): NBTBase = storage.serializeNBT()

  override def handleNBT(nbt: NBTBase): Unit = {
    storage.deserializeNBT(nbt.asInstanceOf[NBTTagCompound])
    value_=(storage)
  }
}
