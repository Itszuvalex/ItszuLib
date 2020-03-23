package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.{SidedFluidStorageConfiguration, TileEntityModule}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

object ModuleIFluidSidedConfiguration {
  val CONFIG_NBT = "Config"
}

class ModuleIFluidSidedConfiguration(val configuration: SidedFluidStorageConfiguration) extends TileEntityModule[SidedFluidStorageConfiguration] {
  override def module: IModule[SidedFluidStorageConfiguration] = ItszuLibModules.FLUID_STORAGE_CONFIGURABLE

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[SidedFluidStorageConfiguration] = _ => Some(configuration)

  override def hasDescriptionNBT: Boolean = true

  override def hasWorldNBT: Boolean = true

  override def writeDescriptionNBT(tag: NBTTagCompound): Unit = writeToNBT(tag)

  override def readDescriptionNBT(tag: NBTTagCompound): Unit = readFromNBT(tag)

  override def writeWorldNBT(tag: NBTTagCompound): Unit = writeToNBT(tag)

  override def readWorldNBT(tag: NBTTagCompound): Unit = readFromNBT(tag)

  def writeToNBT(tag: NBTTagCompound): Unit = {
    tag.setTag(ModuleIFluidSidedConfiguration.CONFIG_NBT, configuration.serializeNBT())
  }

  def readFromNBT(tag: NBTTagCompound): Unit = {
    configuration.deserializeNBT(tag.getCompoundTag(ModuleIFluidSidedConfiguration.CONFIG_NBT))
  }
}
