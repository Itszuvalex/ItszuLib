package com.itszuvalex.itszulib.core

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class NamedStaticBoundingBoxCollection(defaultBoundingBox: () => KeyedBoundingBox, b: Iterable[KeyedBoundingBox]) extends NamedBoundingBoxCollection(defaultBoundingBox) {
  val buffer: ArrayBuffer[KeyedBoundingBox] = new ArrayBuffer[KeyedBoundingBox]()
  buffer ++= b

  override def boxes: mutable.Buffer[KeyedBoundingBox] = buffer
}
