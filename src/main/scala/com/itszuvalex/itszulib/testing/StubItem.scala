package com.itszuvalex.itszulib.testing

import net.minecraft.item.{Item, ItemStack}

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/2016.
  */

/**
  * Add additional default parameters for overrides as we need them.  Use by-name parameters as needed.
  */
class StubItem(stackLimit: Int = 64) extends Item {
  override def getItemStackLimit(stack: ItemStack): Int = stackLimit
}
