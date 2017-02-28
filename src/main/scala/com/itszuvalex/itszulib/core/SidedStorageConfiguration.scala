package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.utility.FacingUtil
import com.itszuvalex.itszulib.util.Debug
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.INBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 2/28/17.
  */
class SidedStorageConfiguration[T >: Null](val defaults: (EnumFacing) => String, val storages: Map[String, T], val front: () => EnumFacing) extends INBTSerializable[NBTTagCompound] {
  val storageSegments = new Array[String](6)

  Debug.only {
    EnumFacing.VALUES.foreach { facing =>
      Debug.assert(storages.keySet.contains(defaults(facing)), "Storages should contain every key in defaults")
    }
  }

  storageSegments.indices.foreach { i =>
    storageSegments(i) = defaults(EnumFacing.VALUES(i))
  }

  def getStorageForGlobalFacing(facing: EnumFacing): T = {
    storages.get(getStorageNameForAbsoluteFacing(facing)).orNull
  }

  def getStorageForRelativeFacing(facing: EnumFacing): T = {
    storages.get(getStorageNameForRelativeFacing(facing)).orNull
  }

  def cycleRelativeFacingForward(facing: EnumFacing): Unit = {
    cycleRelativeFacing(facing, forward = true)
  }

  def cycleRelativeFacingBackward(facing: EnumFacing): Unit = {
    cycleRelativeFacing(facing, forward = false)
  }

  def getStorageNameForRelativeFacing(facing: EnumFacing): String = {
    storageSegments(facing.getIndex)
  }

  def getStorageNameForAbsoluteFacing(facing: EnumFacing): String = {
    storageSegments(FacingUtil.getHorizontalRelativeFacingFromAbsolute(facing, front()).getIndex)
  }

  private def cycleRelativeFacing(facing: EnumFacing, forward: Boolean): Unit = {
    val invKey = storageSegments(facing.getIndex)
    val keys = storages.keys.toArray
    val invInd = keys.indexOf(invKey)
    val shift = if (forward) 1 else -1
    storageSegments(facing.getIndex) = keys(((invInd + shift) + keys.length) % keys.length)
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    storageSegments.indices.foreach { i =>
      storageSegments(i) = nbt.getString(i.toString)
    }
  }

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    storageSegments.indices.foreach { i =>
      nbt.setString(i.toString, storageSegments(i))
    }
    nbt
  }
}
