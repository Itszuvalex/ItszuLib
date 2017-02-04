package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.api.core.{Loc4, Saveable}
import com.itszuvalex.itszulib.api.multiblock.{IMultiBlockComponent, MultiBlockInfo}
import com.itszuvalex.itszulib.core.TileEntityBase
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 12/7/2014.
  */
trait MultiBlockComponent extends TileEntityBase with IMultiBlockComponent {
  @Saveable(desc = true) val info = new MultiBlockInfo

  def formMultiBlock(loc: Loc4): Boolean = {
    val result = info.formMultiBlock(loc)
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

  def getInfo = info

  def forwardToController[T, B](f: T => B): B = {
    if (isValidMultiBlock)
      info.cLoc.getTileEntity(true) match {
        case Some(a: T) => return f(a)
        case _ =>
      }
    null.asInstanceOf[B]
  }

  def forwardToController[T](f: T => Unit): Unit = {
    if (isValidMultiBlock)
      info.cLoc.getTileEntity(true) match {
        case Some(a: T) => f(a)
        case _ =>
      }
  }

  def isValidMultiBlock = info.isValidMultiBlock

  def isController = info.isController(getLoc)
}
