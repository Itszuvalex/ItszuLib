package com.itszuvalex.itszulib.core.behaviors

import com.itszuvalex.itszulib.core.IBlockBehavior

object BlockBehaviors {
  val DEFAULT          : IBlockBehavior = new BlockBehaviorDefault
  val FACING_HORIZONTAL: IBlockBehavior = new BlockBehaviorHorizontalFacing
}
