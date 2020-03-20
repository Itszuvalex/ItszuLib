package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import net.minecraft.nbt.NBTTagCompound

import scala.collection.{SeqView, immutable}

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


  /**
    *
    * @param resource
    * @param doFill
    * @return Amount of resource filled
    */
  def fill(resource: IFluidStack, doFill: Boolean): Int = {
    val amtToFill = resource.amount
    var amtFilled = 0

    // Match to tanks already containing the fluid
    fillable(resource).filter(i => resource.isFluidEqual(apply(i))).exists { i =>
      val stack               = apply(i)
      val cap                 = Math.min(capacity(i), stack.amountMax)
      val room                = cap - stack.amount
      val amtToFillInThisSlot = Math.min(amtToFill - amtFilled, room)
      if (doFill) {
        val copy = apply(i).copy()
        copy.amount += amtToFillInThisSlot
        update(i, copy)
      }
      amtFilled += amtToFillInThisSlot
      (amtToFill - amtFilled) <= 0
    }

    // If we filled up all tanks already containing the fluid, move to empty tanks
    if ((amtToFill - amtFilled) > 0) {
      fillable(resource).filter(i => apply(i).isEmpty).exists { i =>
        val stack               = apply(i)
        val cap                 = Math.min(capacity(i), stack.amountMax)
        val amtToFillInThisSlot = Math.min(amtToFill - amtFilled, cap)
        if (doFill) {
          val copy = resource.copy()
          copy.amount = amtToFillInThisSlot
          update(i, copy)
        }
        amtFilled += amtToFillInThisSlot
        (amtToFill - amtFilled) <= 0
      }
    }

    amtFilled
  }

  private def fillable(resource: IFluidStack): SeqView[Int, immutable.IndexedSeq[Int]] =
    indices.view.filter(i => canFillFluidType(i, resource)).filter(i => apply(i).isEmpty || resource.isFluidEqual(apply(i)))

  /**
    *
    * @param resource
    * @param doDrain
    * @return Amount of resource drained
    */
  def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = {
    val amtToDrain = resource.amount
    var amtDrained = 0
    val ret        = resource.copy()

    // Match to tanks already containing the fluid
    drainable(resource).filter(i => resource.isFluidEqual(apply(i))).exists { i =>
      val stack                = apply(i)
      val amtToDrainInThisSlot = Math.min(amtToDrain - amtDrained, stack.amount)
      if (doDrain) {
        val copy = apply(i).copy()
        copy.amount -= amtToDrainInThisSlot
        if (copy.isEmpty)
          update(i, IFluidStack.Empty)
        else
          update(i, copy)
      }
      amtDrained += amtToDrainInThisSlot
      (amtToDrain - amtDrained) <= 0
    }

    ret.amount = amtDrained
    ret
  }


  def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = {
    var amtDrained       = 0
    var ret: IFluidStack = IFluidStack.Empty

    var firstIndex: Int = 0

    // Get first non empty tank
    indices.view.filter(i => canDrain(i) && !apply(i).isEmpty).exists { i =>
      val stack      = apply(i)
      val amtToDrain = Math.min(maxDrain - amtDrained, stack.amount)
      ret = stack.copy()
      if (doDrain) {
        val copy = stack.copy()
        copy.amount -= amtToDrain
        if (copy.isEmpty)
          update(i, IFluidStack.Empty)
        else
          update(i, copy)
      }
      ret.amount = amtToDrain
      firstIndex = i
      amtDrained += amtToDrain
      true // True here so that we only hit the first non-empty tank
    }

    // We drained something but not all of it, so now drain the rest now we know what liquid we're draining
    if (amtDrained > 0 && (maxDrain - amtDrained) > 0) {
      // We need the index check for the !doDrain scenario, where we might revisit the first tank we simulated draining from
      drainable(ret).filter(i => ret.isFluidEqual(apply(i))).filter(_ != firstIndex).exists { i =>
        val stack                = apply(i)
        val amtToDrainInThisSlot = Math.min(maxDrain - amtDrained, stack.amount)
        if (doDrain) {
          val copy = apply(i).copy()
          copy.amount -= amtToDrainInThisSlot
          if (copy.isEmpty)
            update(i, IFluidStack.Empty)
          else
            update(i, copy)
        }
        amtDrained += amtToDrainInThisSlot
        (maxDrain - amtDrained) <= 0
      }
    }

    if (!ret.isEmpty)
      ret.amount = amtDrained
    ret
  }

  private def drainable(resource: IFluidStack): SeqView[Int, immutable.IndexedSeq[Int]] =
    indices.view.filter(i => canDrainFluidType(i, resource)).filterNot(i => apply(i).isEmpty)

  override def deserializeNBT(t: NBTTagCompound): Unit = {
    indices.
    filter(i =>
             t.hasKey(i.toString)).
    view.
    foreach(i =>
              update(i, readFluidFromSlot(t, i)))
  }

  override def serializeNBT(): NBTTagCompound = {
    val ret = new NBTTagCompound
    zipWithIndex.
    filterNot { case (it: IFluidStack, i: Int) =>
      it.isEmpty
    }
    .view
    .foreach { case (it: IFluidStack, i: Int) => writeFluidToNBT(ret, it, i) }
    ret
  }

  def writeFluidToNBT(nbt: NBTTagCompound, item: IFluidStack, slot: Int): Unit = nbt.setTag(slot.toString, item.serializeNBT())

  def readFluidFromSlot(NBTTagCompound: NBTTagCompound, slot: Int): IFluidStack = IFluidStack.createFromNBT(NBTTagCompound.getCompoundTag(slot.toString))

}
