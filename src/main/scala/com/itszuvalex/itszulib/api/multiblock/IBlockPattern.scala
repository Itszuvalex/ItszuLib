package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.core.{ChunkCoord, Loc4}
import com.itszuvalex.itszulib.api.wrappers.IWorld
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos

trait IBlockPattern {

  def rotation: Rotation

  def canRotate: Boolean

  def rotate(rot: Rotation): Unit

  def rotated(rot: Rotation): IBlockPattern = {
    val ret = copy()
    copy().rotate(rot)
    ret
  }

  def matches(pos: Loc4): Boolean = matches(pos.getWorld.get, pos.getPos)

  def matches(world: IWorld, pos: BlockPos): Boolean

  /**
    *
    * @param pos
    * @return Iterable of actual world BlockPos of all blocks that would be used to make a multiblock with controller at pos
    */
  def blocksInMatch(pos: Loc4): Iterable[BlockPos] = blocksInMatch(pos.getWorld.get, pos.getPos)

  /**
    *
    * @param world
    * @param pos
    * @return Iterable of actual world BlockPos of all blocks that would be used to make a multiblock with controller at pos
    */
  def blocksInMatch(world: IWorld, pos: BlockPos): Iterable[BlockPos]

  def blocksIfMatch(pos: Loc4): Option[Iterable[BlockPos]] = if (matches(pos)) Some(blocksInMatch(pos)) else None

  def blocksIfMatch(world: IWorld, pos: BlockPos): Option[Iterable[BlockPos]] = if (matches(world, pos)) Some(blocksInMatch(world, pos)) else None

  def copy(): IBlockPattern

  def overChunkBoundaries(pos: Loc4): Boolean = overChunkBoundaries(pos.getWorld.get, pos.getPos)

  def overChunkBoundaries(world: IWorld, pos: BlockPos): Boolean = {
    val blocksOpt = blocksIfMatch(world, pos)
    if (blocksOpt.isEmpty) return false
    val blocks = blocksOpt.get
    if (blocks.isEmpty) return false
    val firstLoc   = blocks.head
    val firstChunk = ChunkCoord(firstLoc)
    blocks.exists(l => ChunkCoord(l) != firstChunk)
  }
}
