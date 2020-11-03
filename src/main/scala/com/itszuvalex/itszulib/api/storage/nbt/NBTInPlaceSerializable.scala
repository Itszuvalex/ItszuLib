package com.itszuvalex.itszulib.api.storage.nbt

import net.minecraft.nbt.{NBTBase, NBTTagCompound}
import net.minecraftforge.common.util.INBTSerializable

trait NBTInPlaceSerializable[N] {

  def serialize(obj: N, tag: NBTTagCompound, key: String): Unit

  def deserializeInstance(obj: N, tag: NBTTagCompound, key: String): Unit
}

object NBTInPlaceSerializable {

  trait INBTSerializableIsNBTInPlaceSerializable[T <: NBTBase] extends NBTInPlaceSerializable[INBTSerializable[T]] {
    override def serialize(obj: INBTSerializable[T], tag: NBTTagCompound, key: String): Unit = tag.setTag(key, obj.serializeNBT())

    override def deserializeInstance(obj: INBTSerializable[T], tag: NBTTagCompound, key: String): Unit = obj.deserializeNBT(tag.getTag(key).asInstanceOf[T])
  }

}
