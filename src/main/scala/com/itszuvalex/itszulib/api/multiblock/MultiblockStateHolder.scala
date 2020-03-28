package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.util.Debug
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable
import org.apache.logging.log4j.Level

object MultiblockStateHolder {
  val STATE_KEY = "MultiblockState"
}

class MultiblockStateHolder[S <: INBTSerializable[NBTTagCompound], T <: ITileEntity](
                                                                                      val thisObj: T,
                                                                                      val fact: () => S,
                                                                                      val info: () => MultiBlockInfo,
                                                                                      val getHolder: (T) => MultiblockStateHolder[S, T]) extends INBTSerializable[NBTTagCompound] {
  private var state: Option[S] = None

  def get: Option[S] =
    if (info().isController) Some(getOrElseUpdateState)
    else
      info().controller.flatMap(_.getITileEntity(true)) match {
        case None => None
        case Some(a: T) => Option(getHolder(a).getOrElseUpdateState)
        case _ => None
      }

  private def getOrElseUpdateState: S = {
    state match {
      case None =>
        state = Some(fact())
        Debug.log(Level.INFO, s"Created MultiblockState $state")
      case Some(_) =>
    }
    state.get
  }

  def hasState: Boolean = state.isDefined

  def tryDoOnState(func: S => Unit): Boolean = {
    get.exists { s => func(s); true }
  }

  def doIfController(func: S => Unit): Boolean = {
    if (info().isController) {
      func(getOrElseUpdateState)
      true
    }
    else false
  }

  override def serializeNBT(): NBTTagCompound = {
    val ret = new NBTTagCompound
    if (info().isController) {
      get.foreach(x => ret.setTag(MultiblockStateHolder.STATE_KEY, x.serializeNBT()))
    }
    ret
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    if (info().isController) {
      get.map(_.deserializeNBT(nbt.getCompoundTag(MultiblockStateHolder.STATE_KEY)))
    }
  }
}
