package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.api.core.{Loc4, Saveable}
import com.itszuvalex.itszulib.api.multiblock.{IMultiBlockComponent, MultiBlockInfo}
import com.itszuvalex.itszulib.core.TileEntityBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

/**
  * Created by Chris on 12/7/2014.
  */
trait MultiBlockComponent extends TileEntityBase with IMultiBlockComponent {
  @Saveable(desc = true) val info = new MultiBlockInfo

  def formMultiBlock(world: World, x: Int, y: Int, z: Int): Boolean = {
    val result = info.formMultiBlock(world, x, y, z)
    getWorldObj.markBlockForUpdate(xCoord, yCoord, zCoord)
    getWorldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getWorldObj.getBlock(xCoord, yCoord, zCoord))
    result
  }

  override def handleDescriptionNBT(compound: NBTTagCompound): Unit = {
    super.handleDescriptionNBT(compound)
    setRenderUpdate()
  }

  def breakMultiBlock(world: World, x: Int, y: Int, z: Int): Boolean = {
    val result = info.breakMultiBlock(world, x, y, z)
    getWorldObj.markBlockForUpdate(xCoord, yCoord, zCoord)
    getWorldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getWorldObj.getBlock(xCoord, yCoord, zCoord))
    result
  }

  def getInfo = info

  def forwardToController[T, B](f: T => B): B = {
    if (isValidMultiBlock)
      Loc4(info.x, info.y, info.z, getWorldObj.provider.dimensionId).getTileEntity(true).orNull match {
        case null =>
        case a: T => return f(a)
        case _ =>
      }
    null.asInstanceOf[B]
  }

  def isValidMultiBlock = info.isValidMultiBlock

  def isController = info.isController(xCoord, yCoord, zCoord)
}
