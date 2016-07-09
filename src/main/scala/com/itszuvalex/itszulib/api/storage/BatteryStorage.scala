package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{FloatingBatteryAccess, IPowerAccess}
import com.itszuvalex.itszulib.api.wrappers.IBattery
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 7/8/2016.
  */
class BatteryStorage(private var battery: IBattery) extends IStorage[BatteryStorage, IPowerAccess, IBattery] {
  override def getAccess: IPowerAccess = new FloatingBatteryAccess(battery)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    battery = IBattery.createFromNBT(nbt)
  }

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    battery.writeToNBT(nbt)
    nbt
  }
}
