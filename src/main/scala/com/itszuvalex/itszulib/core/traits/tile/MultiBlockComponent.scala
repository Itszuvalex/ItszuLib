package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.multiblock.{IMultiBlockComponent, MultiBlockInfo}
import com.itszuvalex.itszulib.core.TileEntityBase
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 12/7/2014.
  */
trait MultiBlockComponent extends TileEntityBase with IMultiBlockComponent {
  val info = new MultiBlockInfo

  override def formMultiBlock(loc: Loc4, cloc: Loc4): Boolean = {
    val result = info.formMultiBlock(loc, cloc)
    setUpdate()
    notifyNeighborsOfChange()
    result
  }

  override def handleDescriptionNBT(compound: NBTTagCompound): Unit = {
    super.handleDescriptionNBT(compound)
    setRenderUpdate()
  }

  override def breakMultiBlock(loc: Loc4): Boolean = {
    val result = info.breakMultiBlock(loc)
    setUpdate()
    notifyNeighborsOfChange()
    result
  }

  override def getInfo = info

  def forwardToController[T, B](f: T => B): B = {
    if (isValidMultiBlock)
      info.cLoc.getITileEntity(true) match {
        case Some(a: T) => return f(a)
        case _ =>
      }
    null.asInstanceOf[B]
  }

  def forwardToController[T](f: T => Unit): Unit = {
    if (isValidMultiBlock)
      info.cLoc.getITileEntity(true) match {
        case Some(a: T) => f(a)
        case _ =>
      }
  }

  override def isValidMultiBlock = info.isValidMultiBlock

  /**
    *
    * @param loc
    *
    * @return true if loc == controller location
    */
  override def isController(loc: Loc4): Boolean = info.isController(loc)

  override def isController = info.isController
}
