package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.core.{EnumAutomaticIO, SidedItemStorageConfiguration}
import net.minecraft.util.EnumFacing

class MultiblockSidedItemStorageConfiguration(val loc: () => Loc4, val info: () => MultiBlockInfo, emptyStorage: String, defaults: (EnumFacing) => String, storages: Map[String, IItemStorage], front: () => EnumFacing) extends SidedItemStorageConfiguration(defaults, storages, front) {
  override def getStorageForGlobalFacing(facing: EnumFacing): IItemStorage = {
    if (MultiblockUtils.isLoc4FacingInMultiblock(loc(), facing, info())) IItemStorage.Empty
    else super.getStorageForGlobalFacing(facing)
  }

  override def cycleRelativeFacingStorageForward(facing: EnumFacing): Unit = {
    if (!MultiblockUtils.isLoc4FacingInMultiblock(loc(), facing, info()))
      super.cycleRelativeFacingStorageForward(facing)
  }

  override def cycleRelativeFacingStorageBackward(facing: EnumFacing): Unit = {
    if (!MultiblockUtils.isLoc4FacingInMultiblock(loc(), facing, info()))
      super.cycleRelativeFacingStorageBackward(facing)
  }

  override def cycleRelativeFacingIOForward(facing: EnumFacing): Unit = {
    if (!MultiblockUtils.isLoc4FacingInMultiblock(loc(), facing, info()))
      super.cycleRelativeFacingIOForward(facing)
  }

  override def cycleRelativeFacingIOBackward(facing: EnumFacing): Unit = {
    if (!MultiblockUtils.isLoc4FacingInMultiblock(loc(), facing, info()))
      super.cycleRelativeFacingIOBackward(facing)
  }

  override def getStorageNameForAbsoluteFacing(facing: EnumFacing): String = {
    if (MultiblockUtils.isLoc4FacingInMultiblock(loc(), facing, info())) emptyStorage
    else super.getStorageNameForAbsoluteFacing(facing)
  }

  override def getIOForAbsoluteFacing(facing: EnumFacing): EnumAutomaticIO = {
    if (MultiblockUtils.isLoc4FacingInMultiblock(loc(), facing, info())) EnumAutomaticIO.NONE
    else super.getIOForAbsoluteFacing(facing)
  }

}

