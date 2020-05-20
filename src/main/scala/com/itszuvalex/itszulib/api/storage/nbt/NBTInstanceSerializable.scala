package com.itszuvalex.itszulib.api.storage.nbt

import java.util.UUID

import net.minecraft.nbt.{NBTBase, NBTTagCompound, NBTTagList}

trait NBTInstanceSerializable[N] {

  def serialize(obj: N, tag: NBTTagCompound, key: String): Unit

  def deserializeInstance(tag: NBTTagCompound, key: String): N

}

object NBTInstanceSerializable {

  trait IntIsNBTInstanceSerializable extends NBTInstanceSerializable[Int] {
    override def serialize(obj: Int, tag: NBTTagCompound, key: String): Unit = tag.setInteger(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): Int = tag.getInteger(key)
  }

  implicit object IntIsNBTInstanceSerializable extends IntIsNBTInstanceSerializable

  trait IntArrayIsNBTInstanceSerializable extends NBTInstanceSerializable[Array[Int]] {
    override def serialize(obj: Array[Int], tag: NBTTagCompound, key: String): Unit = tag.setIntArray(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): Array[Int] = tag.getIntArray(key)
  }

  implicit object IntArrayIsNBTInstanceSerializable extends IntArrayIsNBTInstanceSerializable

  trait BooleanIsNBTInstanceSerializable extends NBTInstanceSerializable[Boolean] {
    override def serialize(obj: Boolean, tag: NBTTagCompound, key: String): Unit = tag.setBoolean(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): Boolean = tag.getBoolean(key)
  }

  implicit object BooleanIsNBTInstanceSerializable extends BooleanIsNBTInstanceSerializable

  trait ByteIsNBTInstanceSerializable extends NBTInstanceSerializable[Byte] {
    override def serialize(obj: Byte, tag: NBTTagCompound, key: String): Unit = tag.setByte(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): Byte = tag.getByte(key)
  }

  implicit object ByteIsNBTInstanceSerializable extends ByteIsNBTInstanceSerializable

  trait ByteArrayIsNBTInstanceSerializable extends NBTInstanceSerializable[Array[Byte]] {
    override def serialize(obj: Array[Byte], tag: NBTTagCompound, key: String): Unit = tag.setByteArray(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): Array[Byte] = tag.getByteArray(key)
  }

  implicit object ByteArrayIsNBTInstanceSerializable extends ByteArrayIsNBTInstanceSerializable

  trait DoubleIsNBTInstanceSerializable extends NBTInstanceSerializable[Double] {
    override def serialize(obj: Double, tag: NBTTagCompound, key: String): Unit = tag.setDouble(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): Double = tag.getDouble(key)
  }

  implicit object DoubleIsNBTInstanceSerializable extends DoubleIsNBTInstanceSerializable

  trait FloatIsNBTInstanceSerializable extends NBTInstanceSerializable[Float] {
    override def serialize(obj: Float, tag: NBTTagCompound, key: String): Unit = tag.setFloat(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): Float = tag.getFloat(key)
  }

  implicit object FloatIsNBTInstanceSerializable extends FloatIsNBTInstanceSerializable

  trait LongIsNBTInstanceSerializable extends NBTInstanceSerializable[Long] {
    override def serialize(obj: Long, tag: NBTTagCompound, key: String): Unit = tag.setLong(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): Long = tag.getLong(key)
  }

  implicit object LongIsNBTInstanceSerializable extends LongIsNBTInstanceSerializable

  trait ShortIsNBTInstanceSerializable extends NBTInstanceSerializable[Short] {
    override def serialize(obj: Short, tag: NBTTagCompound, key: String): Unit = tag.setShort(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): Short = tag.getShort(key)
  }

  implicit object ShortIsNBTInstanceSerializable extends ShortIsNBTInstanceSerializable

  trait StringIsNBTInstanceSerializable extends NBTInstanceSerializable[String] {
    override def serialize(obj: String, tag: NBTTagCompound, key: String): Unit = tag.setString(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): String = tag.getString(key)
  }

  implicit object StringIsNBTInstanceSerializable extends StringIsNBTInstanceSerializable

  trait NBTTagCompoundIsNBTInstanceSerializable extends NBTInstanceSerializable[NBTTagCompound] {
    override def serialize(obj: NBTTagCompound, tag: NBTTagCompound, key: String): Unit = tag.setTag(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): NBTTagCompound = tag.getCompoundTag(key)
  }

  implicit object NBTTagCompoundIsNBTInstanceSerializable extends NBTTagCompoundIsNBTInstanceSerializable

  trait NBTTagListIsNBTInstanceSerializable extends NBTInstanceSerializable[NBTTagList] {
    override def serialize(obj: NBTTagList, tag: NBTTagCompound, key: String): Unit = tag.setTag(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): NBTTagList = tag.getTagList(key, 10) // Hard coded to only accept taglists of compound tags
  }

  implicit object NBTTagListIsNBTInstanceSerializable extends NBTTagListIsNBTInstanceSerializable

  trait UUIDIsNBTInstanceSerializable extends NBTInstanceSerializable[UUID] {
    override def serialize(obj: UUID, tag: NBTTagCompound, key: String): Unit = tag.setUniqueId(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): UUID = tag.getUniqueId(key)
  }

  implicit object UUIDIsNBTInstanceSerializable extends UUIDIsNBTInstanceSerializable

  trait NBTBaseIsNBTInstanceSerializable extends NBTInstanceSerializable[NBTBase] {
    override def serialize(obj: NBTBase, tag: NBTTagCompound, key: String): Unit = tag.setTag(key, obj)

    override def deserializeInstance(tag: NBTTagCompound, key: String): NBTBase = tag.getTag(key)
  }

  implicit object NBTBaseIsNBTInstanceSerializable extends NBTBaseIsNBTInstanceSerializable

}
