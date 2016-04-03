package com.itszuvalex.itszulib.api.multiblock

import net.minecraft.util.BlockPos
import net.minecraft.world.World

/**
  * @author Itszuvalex Helper interface for better structure of MultiBlock behavior classes
  *
  *         All methods but formMultiBlockWithBlock assume coordinates given are for the controlling block.
  */
trait IMultiBlock {
  /**
    * @param world
    * @param pos
    * @param strict Set to true to return false if any blocks that would be used to form this multiblock are already in one.
    * @return True if this MultiBlock can form in the given world, with the block at x,y,z as its controller block.
    *
    */
  def canForm(world: World, pos: BlockPos, strict: Boolean): Boolean

  /**
    * @param world
    * @param pos
    * @param cPos
    * @return True if the block at pos is in the MultiBlock with the controller at cPos
    */
  def isBlockInMultiBlock(world: World, pos: BlockPos, cPos: BlockPos): Boolean

  /**
    * @param world
    * @param pos
    * @return True if this MultiBlock correctly forms in the given world, with the block at x,y,z as the controller
    *         block.
    */
  def formMultiBlock(world: World, pos: BlockPos): Boolean

  /**
    * @param world
    * @param pos
    * @return True if this MultiBlock correctly forms in the given world, using the block given at x,y,z anywhere in
    *         the MultiBlock
    */
  def formMultiBlockWithBlock(world: World, pos: BlockPos): Boolean

  /**
    * @param world
    * @param pos
    * @return True if this MultiBlock breaks with no errors in the given world, using the block at x,y,z as the
    *         controller block.
    */
  def breakMultiBlock(world: World, pos: BlockPos): Boolean
}
