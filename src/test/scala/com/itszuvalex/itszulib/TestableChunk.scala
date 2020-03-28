package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.core.ChunkCoord
import com.itszuvalex.itszulib.api.wrappers.{IChunk, IWorld}
import net.minecraft.world.chunk.Chunk

class TestableChunk(private val w: IWorld, private val coord: ChunkCoord) extends IChunk {
  override def x: Int = coord.chunkX

  override def z: Int = coord.chunkZ

  override def world: IWorld = w

  override def toMinecraft: Chunk = null
}
