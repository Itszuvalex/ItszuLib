package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.storage.IItemStorage
import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandlerModifiable

/**
  * Created by Chris on 7/31/2016.
  */
class WrapperItemHandlerModifiable(storage: IItemStorage) extends IItemHandlerModifiable {
  override def setStackInSlot(slot: Int, stack: ItemStack): Unit = storage(slot) = WrapperVanillaItemStack(stack)

  override def getSlots: Int = storage.length

  override def insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack = {
    if (simulate) {
      if (stack == null) stack
      else {
        val islot = storage(slot).copy().toMinecraft
        if (!(ItemStack.areItemsEqual(stack, islot) && ItemStack.areItemStackTagsEqual(stack, islot))) {
          stack
        }
        else {
          val room = storage.maxStackSize(slot) - islot.stackSize
          if (room >= stack.stackSize) null
          else {
            val ret = stack.copy()
            ret.stackSize = ret.stackSize - room
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
      if (storage(slot).isEmpty) null
      else {
        val ret = storage(slot).copy().toMinecraft
        ret.stackSize = Math.min(ret.stackSize, amount)
        ret
      }
    }
    else {
      storage.split(slot, amount).toMinecraft
    }
  }
}
