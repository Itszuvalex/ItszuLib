package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.world.chunk.Chunk

class WrapperChunk(private val chunk: Chunk) extends IChunk {
  override def x: Int = chunk.x

  override def z: Int = chunk.z

  override def world: IWorld = new WrapperWorld(chunk.getWorld)

  override def toMinecraft: Chunk = chunk
}
