package com.itszuvalex.itszulib.api.core

import net.minecraft.nbt.NBTBase

/**
  * Created by Chris on 2/20/2017.
  */
trait INBTObjectSerializer[T, N <: NBTBase] {
  def serialize(obj: T, nbt: N)

  def deserialize(nbt: N): T
}
