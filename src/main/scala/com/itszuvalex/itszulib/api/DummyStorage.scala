package com.itszuvalex.itszulib.api

import net.minecraft.nbt.{NBTBase, NBTTagCompound}
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

abstract class DummyStorage[T] extends Capability.IStorage[T] {
  override def writeNBT(capability: Capability[T], instance: T, side: EnumFacing): NBTBase = {new NBTTagCompound}

  override def readNBT(capability: Capability[T], instance: T, side: EnumFacing, nbt: NBTBase): Unit = {}
}
