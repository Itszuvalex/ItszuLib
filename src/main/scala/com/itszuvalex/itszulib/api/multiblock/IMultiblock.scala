package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.wrappers.IWorld
import net.minecraft.util.math.BlockPos


trait IMultiblock {
  def pattern: IBlockPattern

  def form(loc: Loc4, pat: IBlockPattern): Boolean = form(loc.getWorld.get, loc.getPos, pat)

  /**
    * Assumes pattern matches world, pos
    *
    * @param world
    * @param pos
    * @param pat
    * @return True if all blocks matching pattern successfully were set to this multiblock.  False if any block errored.
    *         Will continue to try and set as many blocks as possible if this occurs.
    */
  def form(world: IWorld, pos: BlockPos, pat: IBlockPattern): Boolean

  def tryForm(loc: Loc4, pat: IBlockPattern): Option[Boolean] = tryForm(loc.getWorld.get, loc.getPos, pat)

  /**
    *
    * @param world
    * @param pos
    * @param pat
    * @return None if pat doesn't match.  Otherwise, the result of form() with the same parameters.
    */
  def tryForm(world: IWorld, pos: BlockPos, pat: IBlockPattern): Option[Boolean]

  def break(loc: Loc4, pat: IBlockPattern): Boolean = break(loc.getWorld.get, loc.getPos, pat)

  def break(world: IWorld, pos: BlockPos, pat: IBlockPattern): Boolean
}
