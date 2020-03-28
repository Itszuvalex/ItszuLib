package com.itszuvalex.itszulib.api.wrappers

import net.minecraft.world.chunk.Chunk

trait IChunk {
  def x: Int

  def z: Int

  def world: IWorld

  def toMinecraft: Chunk
}
