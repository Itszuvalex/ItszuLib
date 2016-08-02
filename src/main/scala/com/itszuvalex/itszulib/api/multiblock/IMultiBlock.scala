package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.core.Loc4

/**
  * @author Itszuvalex Helper interface for better structure of MultiBlock behavior classes
  *
  *         All methods but formMultiBlockWithBlock assume coordinates given are for the controlling block.
  */
trait IMultiBlock {
  /**
    * @param loc
    * @param strict Set to true to return false if any blocks that would be used to form this multiblock are already in one.
    * @return True if this MultiBlock can form in the given world, with the block at x,y,z as its controller block.
    *
    */
  def canForm(loc: Loc4, strict: Boolean): Boolean

  /**
    * @param loc
    * @param controllerLoc
    * @return True if the block at pos is in the MultiBlock with the controller at cPos
    */
  def isBlockInMultiBlock(loc: Loc4, controllerLoc: Loc4): Boolean

  /**
    * @param loc
    * @return True if this MultiBlock correctly forms in the given world, with the block at x,y,z as the controller
    *         block.
    */
  def formMultiBlock(loc: Loc4): Boolean

  /**
    * @param loc
    * @return True if this MultiBlock correctly forms in the given world, using the block given at x,y,z anywhere in
    *         the MultiBlock
    */
  def formMultiBlockWithBlock(loc: Loc4): Boolean

  /**
    * @param loc
    * @return True if this MultiBlock breaks with no errors in the given world, using the block at x,y,z as the
    *         controller block.
    */
  def breakMultiBlock(loc: Loc4): Boolean
}
