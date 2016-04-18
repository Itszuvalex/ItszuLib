package com.itszuvalex.itszulib

import net.minecraft.item.ItemStack

/**
  * Created by Chris on 4/17/2016.
  */
package object wrappers {

  implicit def WrapItemStack(item: ItemStack): WrappedItemStack = new WrappedItemStack(item)
}
