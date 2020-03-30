package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers.IWorld
import net.minecraft.util.math.BlockPos

import scala.collection.mutable

class NamedDynamicBoundingBoxCollection(defaultBoundingBox: () => KeyedBoundingBox, b: (IWorld, BlockPos) => Iterable[KeyedBoundingBox]) extends NamedBoundingBoxCollection(defaultBoundingBox) {
  override def boxes(world: IWorld, pos: BlockPos): mutable.Buffer[KeyedBoundingBox] = b(world, pos).toBuffer
}
