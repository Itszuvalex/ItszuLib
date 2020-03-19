package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.TileEntityModule
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

object ModuleIItemStorage {
  val NBT_KEY = "ItemStorage"
}

class ModuleIItemStorage(val storage: IItemStorage) extends TileEntityModule[IItemStorage] {
  override def module: IModule[IItemStorage] = ItszuLibModules.ITEM_STORAGE

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[IItemStorage] = {
    case null => Some(storage)
    case f =>
      tile.moduleOption(ItszuLibModules.ITEM_STORAGE_CONFIGURABLE, null).map(io => Option(io.getStorageForGlobalFacing(f))).getOrElse(Option(storage))
  }

  override def hasWorldNBT: Boolean = true

  override def writeWorldNBT(tag: NBTTagCompound): Unit = {
    tag.setTag(ModuleIItemStorage.NBT_KEY, storage.serializeNBT())
    super.writeWorldNBT(tag)
  }

  override def readWorldNBT(tagCompound: NBTTagCompound): Unit = {
    storage.deserializeNBT(tagCompound.getCompoundTag(ModuleIItemStorage.NBT_KEY))
    super.readWorldNBT(tagCompound)
  }
}
