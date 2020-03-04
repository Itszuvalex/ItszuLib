package com.itszuvalex.itszulib.api.core

import net.minecraft.util.math.BlockPos

object ChunkCoord {
  def apply(pos: BlockPos): ChunkCoord = ChunkCoord(pos.getX >> 4, pos.getZ >> 4)
}

case class ChunkCoord(x: Int, z: Int)
