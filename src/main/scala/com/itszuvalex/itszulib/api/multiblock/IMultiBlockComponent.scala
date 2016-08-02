package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.core.Loc4

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
    * @param loc
    * @return True if correctly forms, given controller block at x,y,z.
    */
  def formMultiBlock(loc: Loc4): Boolean

  /**
    * @param loc
    * @return True if breaks without errors, given controller block at x,y,z.
    */
  def breakMultiBlock(loc: Loc4): Boolean

  /**
    * @return MultiBlockInfo associated with this MultiBlockComponent
    */
  def getInfo: MultiBlockInfo
}
