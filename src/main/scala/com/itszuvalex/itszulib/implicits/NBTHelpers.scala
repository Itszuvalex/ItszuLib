package com.itszuvalex.itszulib.implicits

import com.itszuvalex.itszulib.api.core.NBTSerializable
import net.minecraft.nbt.{NBTBase, NBTTagCompound, NBTTagList}

import scala.collection.JavaConversions._
import scala.collection.TraversableOnce

/**
  * Created by Christopher on 8/17/2015.
  */
object NBTHelpers {

  object NBTLiterals {

    def NBTCompound(serializable: NBTSerializable) = {
      val compound = new NBTTagCompound
      serializable.saveToNBT(compound)
      compound
    }

    def NBTCompound(elems: (String, Any)*): NBTTagCompound = NBTAdditions.NBTCompoundAdding(new NBTTagCompound)(elems: _*)

    def NBTList(collection: TraversableOnce[_ <: NBTBase]) = {
      val list = new NBTTagList
      collection.foreach(list.appendTag)
      list
    }

    def NBTList(elems: NBTBase*): NBTTagList = {
      val list = new NBTTagList
      elems.foreach(list.appendTag)
      list
    }

  }

  object NBTAdditions {

    implicit class NBTListAdding(list: NBTTagList) {
      def +=(tag: NBTBase) = list.appendTag(tag)

      def ++=(xs: TraversableOnce[_ <: NBTBase]) = xs.foreach(list.appendTag)
    }

    implicit class NBTListIterable(list: NBTTagList) extends Iterable[NBTTagCompound] {
      override def iterator: Iterator[NBTTagCompound] = new NBTListIterator(this.list)

      class NBTListIterator(private val list: NBTTagList) extends Iterator[NBTTagCompound] {
        var index = 0

        override def hasNext = index < list.tagCount()

        override def next(): NBTTagCompound = {
          val ret = list.getCompoundTagAt(index)
          index += 1
          ret
        }
      }

    }

    implicit class NBTCompoundAdding(compound: NBTTagCompound) {
      def apply(elems: (String, Any)*): NBTTagCompound = {
        elems.foreach { case (key, value) => value match {
          case null =>
          case b: Boolean => compound.setBoolean(key, b)
          case b: Byte => compound.setByte(key, b)
          case ba: Array[Byte] => compound.setByteArray(key, ba)
          case d: Double => compound.setDouble(key, d)
          case f: Float => compound.setFloat(key, f)
          case ia: Array[Int] => compound.setIntArray(key, ia)
          case i: Int => compound.setInteger(key, i)
          case l: Long => compound.setLong(key, l)
          case s: Short => compound.setShort(key, s)
          case s: String => compound.setString(key, s)
          case n: NBTBase => compound.setTag(key, n)
          case save: NBTSerializable =>
            compound.setTag(key, NBTLiterals.NBTCompound(save))
          case _ =>
        }
                      }
        compound
      }

      def merge(elems: (String, Any)*): NBTTagCompound = {
        elems.foreach { case (key, value) => value match {
          case null =>
          case n: NBTTagCompound =>
            if (compound.hasKey(key)) {
              val nc = compound.getCompoundTag(key)
              nc.merge(n.func_150296_c().collect { case key: String => (key, n.getTag(key)) }.toSeq: _*)
            }
            else {
              compound.setTag(key, n)
            }
          case _ => apply((key, value))
        }
                      }
        compound
      }
    }


    implicit class NBTCompoundReading(compound: NBTTagCompound) {

      def Bool(key: String) = compound.getBoolean(key)

      def Byte(key: String) = compound.getByte(key)

      def ByteArray(key: String) = if (compound.hasKey(key)) compound.getByteArray(key) else null

      def Double(key: String) = compound.getDouble(key)

      def Float(key: String) = compound.getFloat(key)

      def IntArray(key: String) = if (compound.hasKey(key)) compound.getIntArray(key) else null

      def Int(key: String) = compound.getInteger(key)

      def Long(key: String) = compound.getLong(key)

      def Short(key: String) = compound.getShort(key)

      def String(key: String) = if (compound.hasKey(key)) compound.getString(key) else null

      def NBTCompound[T <: AnyRef](key: String)(callback: NBTTagCompound => T): T = if (compound != null) {
        if (compound.hasKey(key)) {
          val read = compound.getCompoundTag(key)
          if (callback != null) callback(read)
          else null.asInstanceOf[T]
        } else null.asInstanceOf[T]
      } else null.asInstanceOf[T]

      def NBTList(key: String) = if (compound.hasKey(key)) compound.getTagList(key, 10) else null

      def NBTTag(key: String) = if (compound.hasKey(key)) compound.getTag(key) else null
    }

  }

}
