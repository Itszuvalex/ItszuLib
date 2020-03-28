package com.itszuvalex.itszulib.api.core

import net.minecraft.util.math.BlockPos

object ChunkCoord {
  def apply(pos: BlockPos): ChunkCoord = ChunkCoord(pos.getX >> 4, pos.getZ >> 4)

  def apply(loc4: Loc4): ChunkCoord = ChunkCoord(loc4.getPos)
}

case class ChunkCoord(chunkX: Int, chunkZ: Int)
