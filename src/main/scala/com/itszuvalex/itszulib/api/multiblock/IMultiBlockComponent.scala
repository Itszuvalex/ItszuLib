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
    *
    * @return true if this is the controller block
    */
  def isController: Boolean

  /**
    *
    * @param loc
    *
    * @return true if loc == controller location
    */
  def isController(loc: Loc4): Boolean

  /**
    * @param loc
    * @param cloc
    *
    * @return True if correctly forms, given controller block at x,y,z.
    */
  def formMultiBlock(loc: Loc4, cloc: Loc4): Boolean

  /**
    * @param cloc
    *
    * @return True if breaks without errors, given controller block at x,y,z.
    */
  def breakMultiBlock(cloc: Loc4): Boolean

  /**
    * @return MultiBlockInfo associated with this MultiBlockComponent
    */
  def getInfo: MultiBlockInfo
}
