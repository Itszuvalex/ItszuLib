package com.itszuvalex.itszulib.api.storage

import net.minecraft.nbt.NBTTagCompound

class DynamicIBattery[N](val getter: () => IBattery[N]) extends IBattery[N] {

  override def amount: N = getter().amount

  override def amount_=(amount: N): Unit = getter().amount_=(amount)

  override def amountMax: N = getter().amountMax

  override def copy(): IBattery[N] = getter().copy()

  override def clear(): Unit = getter().clear()

  override def writeToNBT(nbt: NBTTagCompound): Unit = getter().writeToNBT(nbt)

  override def deserializeNBT(nbt: NBTTagCompound): Unit = getter().deserializeNBT(nbt)

  override def serializeNBT(): NBTTagCompound = getter().serializeNBT()

  override def fill(amt: N)(implicit n: Numeric[N]): N = getter().fill(amt)

  override def drain(amt: N)(implicit n: Numeric[N]): N = getter().drain(amt)
}
