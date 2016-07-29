package com.itszuvalex.itszulib.network.messages

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import io.netty.buffer.ByteBuf
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}
import net.minecraftforge.fml.common.network.simpleimpl.IMessage

/**
  * Created by Alex on 11.10.2015.
  */
abstract class MessageUpdateNBT[REQ <: IMessage, REP <: IMessage](var nbt: NBTTagCompound) extends MessageBase[REQ, REP] {
  def this() = this(null)

  override def toBytes(buf: ByteBuf): Unit = {
    if (nbt == null) {
      buf.writeShort(-1)
    }
    else {
      val stream = new ByteArrayOutputStream()
      CompressedStreamTools.writeCompressed(nbt, stream)
      val abyte: Array[Byte] = stream.toByteArray
      buf.writeShort(abyte.length.toShort)
      buf.writeBytes(abyte)
    }
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    val short1: Int = buf.readShort

    if (short1 < 0) {
      nbt = null
    }
    else {
      val abyte: Array[Byte] = new Array[Byte](short1)
      buf.readBytes(abyte)
      nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(abyte))
    }
  }
}
