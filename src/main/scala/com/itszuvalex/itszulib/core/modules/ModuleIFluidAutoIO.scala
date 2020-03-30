package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.modules.ModuleIFluidAutoIO._
import com.itszuvalex.itszulib.core.{SidedFluidStorageConfiguration, TileEntityInternalModuleTickable}
import com.itszuvalex.itszulib.util.TileEntityUtils
import net.minecraft.nbt.NBTTagCompound

object ModuleIFluidAutoIO {
  val TICKS_DEFAULT = 20
  val AMT_DEFAULT   = 250

  val TICKS_NBT        = "Ticks"
  val TICKS_PER_OP_NBT = "TicksPerOp"
  val AMT_PER_OP_NBT   = "AmtPerOp"
}

class ModuleIFluidAutoIO(val sidedConfig: SidedFluidStorageConfiguration, var ticksPerOperation: () => Int = TICKS_DEFAULT _, var amtPerOperation: () => Int = AMT_DEFAULT _) extends TileEntityInternalModuleTickable[ModuleIFluidAutoIO] {
  var ticks = 0

  override def module: IModule[ModuleIFluidAutoIO] = InternalModules.MODULE_FLUID_AUTO_IO

  override def serverUpdate(tile: ITileEntity): Unit = {
    ticks = TileEntityUtils.incrementTicks(ticks, ticksPerOperation())
    TileEntityUtils.checkDoFluidInputIO(tile, sidedConfig, ticks, amtPerOperation())
    TileEntityUtils.checkDoFluidOutputIO(tile, sidedConfig, ticks, amtPerOperation())
  }

  override def hasWorldNBT: Boolean = true

  override def writeWorldNBT(tag: NBTTagCompound): Unit = {
    tag.setInteger(TICKS_NBT, ticks)
  }

  override def readWorldNBT(tagCompound: NBTTagCompound): Unit = {
    ticks = tagCompound.getInteger(TICKS_NBT)
  }
}
