package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.modules.ModuleIItemAutoIO._
import com.itszuvalex.itszulib.core.{SidedItemStorageConfiguration, TileEntityInternalModuleTickable}
import com.itszuvalex.itszulib.util.TileEntityUtils
import net.minecraft.nbt.NBTTagCompound

object ModuleIItemAutoIO {
  val TICKS_DEFAULT = 20
  val AMT_DEFAULT   = 1

  val TICKS_NBT        = "Ticks"
  val TICKS_PER_OP_NBT = "TicksPerOp"
  val AMT_PER_OP_NBT   = "AmtPerOp"
}

class ModuleIItemAutoIO(val sidedConfig: SidedItemStorageConfiguration, var ticksPerOperation: () => Int = TICKS_DEFAULT _, var amtPerOperation: () => Int = AMT_DEFAULT _) extends TileEntityInternalModuleTickable[ModuleIItemAutoIO] {
  var ticks = 0

  override def module: IModule[ModuleIItemAutoIO] = InternalModules.MODULE_ITEM_AUTO_IO

  override def serverUpdate(tile: ITileEntity): Unit = {
    ticks = TileEntityUtils.incrementTicks(ticks, ticksPerOperation())
    TileEntityUtils.checkDoItemInputIO(tile, sidedConfig, ticks, amtPerOperation())
    TileEntityUtils.checkDoItemOutputIO(tile, sidedConfig, ticks, amtPerOperation())
  }

  override def hasWorldNBT: Boolean = true

  override def writeWorldNBT(tag: NBTTagCompound): Unit = {
    tag.setInteger(TICKS_NBT, ticks)
  }

  override def readWorldNBT(tagCompound: NBTTagCompound): Unit = {
    ticks = tagCompound.getInteger(TICKS_NBT)
  }
}
