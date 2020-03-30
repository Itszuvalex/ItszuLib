package com.itszuvalex.itszulib.core

import scala.collection.mutable

class NamedDynamicBoundingBoxCollection(defaultBoundingBox: () => KeyedBoundingBox, b: () => Iterable[KeyedBoundingBox]) extends NamedBoundingBoxCollection(defaultBoundingBox) {
  override def boxes: mutable.Buffer[KeyedBoundingBox] = b().toBuffer
}
