package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.multiblock.MultiBlockInfo
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.TileEntityModule
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

object ModuleMultiblockInfo {
  val INFO_KEY = "Info"
}

class ModuleMultiblockInfo(val info: MultiBlockInfo) extends TileEntityModule[MultiBlockInfo] {
  override def module: IModule[MultiBlockInfo] = ItszuLibModules.TILE_MULTIBLOCK

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[MultiBlockInfo] = _ => Some(info)

  override def hasDescriptionNBT: Boolean = true

  override def hasWorldNBT: Boolean = true

  override def writeDescriptionNBT(tag: NBTTagCompound): Unit = saveToNBT(tag)

  override def readDescriptionNBT(tag: NBTTagCompound): Unit = readFromNbt(tag)

  override def writeWorldNBT(tag: NBTTagCompound): Unit = saveToNBT(tag)

  override def readWorldNBT(tagCompound: NBTTagCompound): Unit = readFromNbt(tagCompound)

  def saveToNBT(nbt: NBTTagCompound): Unit = {
    nbt.setTag(ModuleMultiblockInfo.INFO_KEY, info.serializeNBT())
  }

  def readFromNbt(nbt: NBTTagCompound): Unit = {
    info.deserializeNBT(nbt.getCompoundTag(ModuleMultiblockInfo.INFO_KEY))
  }
}
