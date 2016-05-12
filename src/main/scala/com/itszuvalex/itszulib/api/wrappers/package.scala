package com.itszuvalex.itszulib.api

import net.minecraft.item.ItemStack

/**
  * Created by Chris on 4/17/2016.
  */
package object wrappers {

  implicit class WrappableItemStack(stack: ItemStack) {
    def wrap = if (stack == null) null else new WrappedItemStack(stack)
  }

  implicit def WrapItemStack(item: ItemStack): WrappedItemStack = new WrappedItemStack(item)

  implicit def WrapItemStackToInterface(item: ItemStack): IItemStack = new WrappedItemStack(item)
}
