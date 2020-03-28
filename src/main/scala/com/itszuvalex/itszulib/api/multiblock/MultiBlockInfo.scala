package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.implicits.NBTHelpers.NBTAdditions._
import com.itszuvalex.itszulib.implicits.NBTHelpers.NBTLiterals._
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

class MultiBlockInfo extends INBTSerializable[NBTTagCompound] {
  private var isMultiBlock        = false
  private var isControl           = false
  private var controllerLoc: Loc4 = new Loc4(0, 0, 0, 0)

  def isValidMultiBlock: Boolean = isMultiBlock

  def formMultiBlock(loc: Loc4, cloc: Loc4): Boolean = {
    if (isMultiBlock) {
      if (loc != controllerLoc) {
        return false
      }
    }
    isMultiBlock = true
    controllerLoc = cloc
    isControl = loc == cloc
    true
  }

  def breakMultiBlock(loc: Loc4): Boolean = {
    if (isMultiBlock) {
      if (loc != controllerLoc) {
        return false
      }
    }
    isMultiBlock = false
    isControl = false
    true
  }

  override def serializeNBT(): NBTTagCompound = {
    NBTCompound(
      "isFormed" -> isMultiBlock,
      "c_loc" -> controllerLoc,
      "controller" -> isControl
      )
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    isMultiBlock = nbt.Bool("isFormed")
    controllerLoc = nbt.NBTCompound("c_loc")(Loc4(_))
    isControl = nbt.Bool("controller")
  }

  /**
    *
    * @return
    */
  def isController: Boolean = isControl

  def controller: Option[Loc4] = if (isMultiBlock) Option(controllerLoc) else None

}
