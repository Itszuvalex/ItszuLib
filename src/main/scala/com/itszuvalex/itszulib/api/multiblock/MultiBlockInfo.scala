package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.implicits.NBTHelpers.NBTAdditions._
import com.itszuvalex.itszulib.implicits.NBTHelpers.NBTLiterals._
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

class MultiBlockInfo extends IMultiBlockComponent with INBTSerializable[NBTTagCompound] {
  private var isMultiBlock        = false
  private var controller          = false
  private var controllerLoc: Loc4 = new Loc4(0, 0, 0, 0)

  override def isValidMultiBlock = isMultiBlock

  override def formMultiBlock(loc: Loc4, cloc: Loc4): Boolean = {
    if (isMultiBlock) {
      if (loc != controllerLoc) {
        return false
      }
    }
    isMultiBlock = true
    controllerLoc = cloc
    controller = loc == cloc
    true
  }

  def cLoc = controllerLoc

  override def breakMultiBlock(loc: Loc4): Boolean = {
    if (isMultiBlock) {
      if (loc != controllerLoc) {
        return false
      }
    }
    isMultiBlock = false
    controller = false
    true
  }

  override def getInfo = this

  override def serializeNBT(): NBTTagCompound = {
    NBTCompound(
      "isFormed" -> isMultiBlock,
      "c_loc" -> controllerLoc,
      "controller" -> controller
    )
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    isMultiBlock = nbt.Bool("isFormed")
    controllerLoc = nbt.NBTCompound("c_loc")(Loc4(_))
    controller = nbt.Bool("controller")
  }

  /**
    *
    * @return
    */
  override def isController: Boolean = controller

  /**
    *
    * @param loc
    *
    * @return true if loc == controller location
    */
  override def isController(loc: Loc4): Boolean = loc == controllerLoc
}
