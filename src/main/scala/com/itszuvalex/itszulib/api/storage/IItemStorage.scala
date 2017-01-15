package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
trait IItemStorage extends scala.collection.immutable.Seq[IItemStack] with INBTSerializable[NBTTagCompound] {

  /**
    * Does not actually prevent inserting.  This should be overridden by inventories that want to prevent insertion by players.
    * Insertion by machines should also check this, but in cases of output slots we can't just prevent insertion of items at the storage level.
    *
    * @param i     Index
    * @param stack Stack to insert
    *
    * @return True if this stack can be inserted.
    */
  def canInsert(i: Int, stack: IItemStack): Boolean = true

  /**
    *
    * @param i Index
    *
    * @return Get IItemStack contained at this location.  This should never return null, as IItemStacks track their own emptiness.
    */
  def apply(i: Int): IItemStack

  /**
    *
    * @return Max stack size allowed in this storage.
    */
  def maxStackSize(i: Int): Int = Math.min(64, apply(i).stackSizeMax)

  /**
    *
    * @param i Index to split
    * @param a Amount to attempt to pull out of this location.
    *
    * @return IItemStack containing the split stack.  This should never return null.
    */
  def split(i: Int, a: Int): IItemStack = {
    val slot = apply(i)
    val ret = slot.copy()
    if (a >= slot.stackSize)
      setSlot(i, IItemStack.Empty)
    else {
      slot.stackSize -= a
      ret.stackSize = a
    }
    ret
  }

  /**
    *
    * @param i Index to insert into
    * @param s ItemStack to attempt to insert.
    *
    * @return IItemStack containing the leftovers from s.  This should never return null.
    */
  def insert(i: Int, s: IItemStack): IItemStack = {
    if (s.isEmpty)
      return s

    val slot = apply(i)
    if (slot.isEmpty) {
      val max = Math.min(s.stackSizeMax, maxStackSize(i))
      if (s.stackSize <= max) {
        setSlot(i, s)
        IItemStack.Empty
      }
      else {
        val slot = s.copy()
        slot.stackSize = max
        val ret = s.copy()
        ret.stackSize -= max
        setSlot(i, slot)
        ret
      }
    }
    else if (slot.isItemEqual(s)) {
      val max = Math.min(s.stackSizeMax, maxStackSize(i))
      val room = max - slot.stackSize
      if (s.stackSize <= room) {
        slot.stackSize += s.stackSize
        IItemStack.Empty
      }
      else {
        val slotcopy = slot.copy()
        slotcopy.stackSize += room
        val ret = s.copy()
        ret.stackSize -= room
        setSlot(i, slotcopy)
        ret
      }
    }
    else s
  }

  /**
    *
    * @param i Index
    * @param s ItemStack to set index = to
    */
  def setSlot(i: Int, s: IItemStack): Unit = update(i, s)

  /**
    *
    * @param i Index to update
    * @param s IItemStack to set
    */
  def update(i: Int, s: IItemStack): Unit

  override def iterator: Iterator[IItemStack] = new ItemStorageIterator(this)

  override def deserializeNBT(t: NBTTagCompound): Unit = {
    indices.
      filter(i =>
        t.hasKey(i.toString)).
      view.
      foreach(i =>
        this (i) = IItemStack.createFromNBT(t.getCompoundTag(i.toString)))
  }

  override def serializeNBT(): NBTTagCompound = {
    val ret = new NBTTagCompound
    zipWithIndex.
      filterNot { case (it: IItemStack, i: Int) =>
        it.isEmpty
      }
      .view
      .foreach { case (it: IItemStack, i: Int) =>
        ret.setTag(i.toString, it.serializeNBT())
      }
    ret
  }
}
