package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.{SidedItemStorageConfiguration, TileEntityModule}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

object ModuleIItemSidedConfiguration {
  val CONFIG_NBT = "Config"
}

class ModuleIItemSidedConfiguration(val configuration: SidedItemStorageConfiguration) extends TileEntityModule[SidedItemStorageConfiguration] {
  override def module: IModule[SidedItemStorageConfiguration] = ItszuLibModules.ITEM_STORAGE_CONFIGURABLE

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[SidedItemStorageConfiguration] = _ => Some(configuration)

  override def hasDescriptionNBT: Boolean = true

  override def hasWorldNBT: Boolean = true

  override def writeDescriptionNBT(tag: NBTTagCompound): Unit = writeToNBT(tag)

  override def readDescriptionNBT(tag: NBTTagCompound): Unit = readFromNBT(tag)

  override def writeWorldNBT(tag: NBTTagCompound): Unit = writeToNBT(tag)

  override def readWorldNBT(tag: NBTTagCompound): Unit = readFromNBT(tag)

  def writeToNBT(tag: NBTTagCompound): Unit = {
    tag.setTag(ModuleIItemSidedConfiguration.CONFIG_NBT, configuration.serializeNBT())
  }

  def readFromNBT(tag: NBTTagCompound): Unit = {
    configuration.deserializeNBT(tag.getCompoundTag(ModuleIItemSidedConfiguration.CONFIG_NBT))
  }
}
