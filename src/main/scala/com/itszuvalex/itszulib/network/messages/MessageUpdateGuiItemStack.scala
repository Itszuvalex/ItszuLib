package com.itszuvalex.itszulib.network.messages

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

/**
  * Created by Alex on 11.10.2015.
  */
class MessageUpdateGuiItemStack(var stack: NBTTagCompound) extends IMessage with IMessageHandler[MessageUpdateGuiItemStack, IMessage] {
  def this() = this(null)

  override def toBytes(buf: ByteBuf): Unit = {
    if (stack == null) {
      buf.writeShort(-1)
    }
    else {
      val stream = new ByteArrayOutputStream()
      CompressedStreamTools.writeCompressed(stack, stream)
      val abyte: Array[Byte] = stream.toByteArray
      buf.writeShort(abyte.length.toShort)
      buf.writeBytes(abyte)
    }
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    val short1: Int = buf.readShort

    if (short1 < 0) {
      stack = null
    }
    else {
      val abyte: Array[Byte] = new Array[Byte](short1)
      buf.readBytes(abyte)
      stack = CompressedStreamTools.readCompressed(new ByteArrayInputStream(abyte))
    }
  }

  override def onMessage(message: MessageUpdateGuiItemStack, ctx: MessageContext): IMessage = {
    Minecraft.getMinecraft.thePlayer.inventory.setItemStack(ItemStack.loadItemStackFromNBT(message.stack))
    null
  }
}
