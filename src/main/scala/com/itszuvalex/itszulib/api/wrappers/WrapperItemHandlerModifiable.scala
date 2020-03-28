package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.storage.IItemStorage
import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandlerModifiable

/**
  * Created by Chris on 7/31/2016.
  */
class WrapperItemHandlerModifiable(storage: IItemStorage) extends IItemHandlerModifiable {
  override def setStackInSlot(slot: Int, stack: ItemStack): Unit = storage(slot) = WrapperVanillaItemStack(stack)

  override def getSlotLimit(slot: Int): Int = 64

  override def getSlots: Int = storage.length

  override def insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack = {
    if (simulate) {
      if (stack == null || stack.isEmpty) stack
      else {
        val islot = storage(slot).copy().toMinecraft
        if (!(ItemStack.areItemsEqual(stack, islot) && ItemStack.areItemStackTagsEqual(stack, islot))) {
          stack
        }
        else {
          val room = storage.maxStackSize(slot) - islot.getCount
          if (room >= stack.getCount) ItemStack.EMPTY
          else {
            val ret = stack.copy()
            ret.setCount(ret.getCount - room)
            ret
          }
        }
      }
    }
    else {
      storage.insert(slot, WrapperVanillaItemStack(stack)).toMinecraft
    }
  }

  override def getStackInSlot(slot: Int): ItemStack = storage(slot).toMinecraft

  override def extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack = {
    if (simulate) {
      if (storage(slot).isEmpty) ItemStack.EMPTY
      else {
        val ret = storage(slot).copy().toMinecraft
        ret.setCount(Math.min(ret.getCount, amount))
        ret
      }
    }
    else {
      storage.split(slot, amount).toMinecraft
    }
  }
}
