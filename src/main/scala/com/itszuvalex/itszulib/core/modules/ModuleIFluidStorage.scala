package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.storage.IFluidStorage
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.TileEntityModule
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

object ModuleIFluidStorage {
  val NBT_KEY = "FluidStorage"
}


class ModuleIFluidStorage(val storage: IFluidStorage) extends TileEntityModule[IFluidStorage] {
  override def module: IModule[IFluidStorage] = ItszuLibModules.FLUID_STORAGE

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[IFluidStorage] = {
    case null => Some(storage)
    case f =>
      tile.moduleOption(ItszuLibModules.FLUID_STORAGE_CONFIGURABLE, null).map(io => Option(io.getStorageForGlobalFacing(f))).getOrElse(Option(storage))
  }

  override def hasWorldNBT: Boolean = true

  override def writeWorldNBT(tag: NBTTagCompound): Unit = {
    tag.setTag(ModuleIFluidStorage.NBT_KEY, storage.serializeNBT())
    super.writeWorldNBT(tag)
  }

  override def readWorldNBT(tagCompound: NBTTagCompound): Unit = {
    storage.deserializeNBT(tagCompound.getCompoundTag(ModuleIFluidStorage.NBT_KEY))
    super.readWorldNBT(tagCompound)
  }
}
