package com.itszuvalex.itszulib.api.multiblock

import net.minecraft.util.BlockPos
import net.minecraft.world.World

/**
  * @author Itszuvalex
  *         Interface for MultiBlock components for easy implementation.
  */
trait IMultiBlockComponent {
  /**
    * @return True if this is in valid MultiBlock
    */
  def isValidMultiBlock: Boolean

  /**
    * @param pos
    * @return True if correctly forms, given controller block at x,y,z.
    */
  def formMultiBlock(world: World, pos: BlockPos): Boolean

  /**
    * @param pos
    * @return True if breaks without errors, given controller block at x,y,z.
    */
  def breakMultiBlock(world: World, pos: BlockPos): Boolean

  /**
    * @return MultiBlockInfo associated with this MultiBlockComponent
    */
  def getInfo: MultiBlockInfo
}
