package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{FloatingBatteryAccess, IPowerAccess}
import com.itszuvalex.itszulib.api.wrappers.IBattery
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Chris on 7/8/2016.
  */
class PowerStorage(private var battery: IBattery) extends IStorage[PowerStorage, IPowerAccess, IBattery] {
  override def getAccess: IPowerAccess = new FloatingBatteryAccess(battery)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    battery = IBattery.createFromNBT(nbt)
    onChanged()
  }

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    battery.writeToNBT(nbt)
    nbt
  }
}
