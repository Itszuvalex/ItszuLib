package com.itszuvalex.itszulib.api

import net.minecraft.item.ItemStack

/**
  * Created by Chris on 4/17/2016.
  */
package object wrappers {

  implicit class WrappableItemStack(stack: ItemStack) {
    def wrap = if (stack == null) null else new WrapperVanillaItemStack(stack)
  }

  implicit def WrapItemStack(item: ItemStack): WrapperVanillaItemStack = new WrapperVanillaItemStack(item)

  implicit def WrapItemStackToInterface(item: ItemStack): IItemStack = new WrapperVanillaItemStack(item)
}
