package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.wrappers.IWorld
import net.minecraft.util.math.BlockPos

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class NamedStaticBoundingBoxCollection(defaultBoundingBox: () => KeyedBoundingBox, b: Iterable[KeyedBoundingBox]) extends NamedBoundingBoxCollection(defaultBoundingBox) {
  val buffer: ArrayBuffer[KeyedBoundingBox] = new ArrayBuffer[KeyedBoundingBox]()
  buffer ++= b

  override def boxes(world: IWorld, pos: BlockPos): mutable.Buffer[KeyedBoundingBox] = buffer
}
