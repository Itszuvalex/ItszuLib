package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
trait IFluidStorage extends scala.collection.immutable.Seq[IFluidStack] with INBTSerializable[NBTTagCompound] {

  /**
    *
    * @param i Index
    * @return Get IFluidStack contained at this location.  This should never return null, as IFluidStacks track their own emptiness.
    */
  def apply(i: Int): IFluidStack

  /**
    *
    * @return Max stack size allowed in this storage.
    */
  def maxAmount(i: Int): Int = Int.MaxValue

  /**
    *
    * @param i Index to split
    * @param a Amount to attempt to pull out of this location.
    * @return IFluidStack containing the split stack.  This should never return null.
    */
  def split(i: Int, a: Int): IFluidStack = {
    val slot = apply(i)
    val ret = slot.copy()
    if (a >= slot.amount)
      setSlot(i, IFluidStack.Empty)
    else {
      slot.amount -= a
      ret.amount = a
    }
    ret
  }

  /**
    *
    * @param i Index to insert into
    * @param s ItemStack to attempt to insert.
    * @return IFluidStack containing the leftovers from s.  This should never return null.
    */
  def insert(i: Int, s: IFluidStack): IFluidStack = {
    if (s.isEmpty)
      return s

    val slot = apply(i)
    if (slot.isEmpty) {
      val max = Math.min(s.amountMax, maxAmount(i))
      if (s.amount <= max) {
        setSlot(i, s)
        IFluidStack.Empty
      }
      else {
        val slot = s.copy()
        slot.amount = max
        val ret = s.copy()
        ret.amount -= max
        setSlot(i, slot)
        ret
      }
    }
    else if (slot.isFluidEqual(s)) {
      val max = Math.min(s.amountMax, maxAmount(i))
      val room = max - slot.amount
      if (s.amount <= room) {
        slot.amount += s.amount
        IFluidStack.Empty
      }
      else {
        val slotcopy = slot.copy()
        slotcopy.amount += room
        val ret = s.copy()
        ret.amount -= room
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
  def setSlot(i: Int, s: IFluidStack): Unit = update(i, s)

  /**
    *
    * @param i Index to update
    * @param s IFluidStack to set
    */
  def update(i: Int, s: IFluidStack): Unit

  override def iterator: Iterator[IFluidStack] = new FluidStorageIterator(this)

  override def deserializeNBT(t: NBTTagCompound): Unit = {
    indices.
    filter(i =>
             t.hasKey(i.toString)).
    view.
    foreach(i =>
              this (i) = IFluidStack.createFromNBT(t.getCompoundTag(i.toString)))
  }

  override def serializeNBT(): NBTTagCompound = {
    val ret = new NBTTagCompound
    zipWithIndex.
    filterNot { case (it: IFluidStack, i: Int) =>
      it.isEmpty
              }
    .view
    .foreach { case (it: IFluidStack, i: Int) =>
      ret.setTag(i.toString, it.serializeNBT())
             }
    ret
  }
}
