package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IItemStack
import net.minecraft.nbt.NBTTagCompound

class DynamicIItemStorage(val getter: () => IItemStorage) extends IItemStorage {
  override def apply(i: Int): IItemStack = getter().apply(i)

  override def update(i: Int, s: IItemStack): Unit = getter().update(i, s)

  override def canInsert(i: Int, stack: IItemStack): Boolean = getter().canInsert(i, stack)

  override def maxStackSize(i: Int): Int = getter().maxStackSize(i)

  override def split(i: Int, a: Int): IItemStack = getter().split(i, a)

  override def insert(i: Int, s: IItemStack): IItemStack = getter().insert(i, s)

  override def setSlot(i: Int, s: IItemStack): Unit = getter().setSlot(i, s)

  override def iterator: Iterator[IItemStack] = getter().iterator

  override def transferSlotIntoStorageSlot(slot: Int, storage: IItemStorage, targetSlot: Int, amt: Int): Int = getter().transferSlotIntoStorageSlot(slot, storage, targetSlot, amt)

  override def transferSlotIntoStorage(slot: Int, storage: IItemStorage, amt: Int): Int = getter().transferSlotIntoStorage(slot, storage, amt)

  override def transferIntoStorage(storage: IItemStorage, amt: Int): Int = getter().transferIntoStorage(storage, amt)

  override def deserializeNBT(t: NBTTagCompound): Unit = getter().deserializeNBT(t)

  override def serializeNBT(): NBTTagCompound = getter().serializeNBT()

  override def length: Int = getter().length
}
