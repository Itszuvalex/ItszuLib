package com.itszuvalex.itszulib.network.messages

import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, MessageContext}

/**
  * Created by Christopher Harris (Itszuvalex) on 10/19/14.
  */
class MessageContainerUpdate(private var index: Int, private var value: Int) extends MessageBase[MessageContainerUpdate, IMessage] {

  def this() = this(0, 0)

  override def fromBytes(buf: ByteBuf) {
    index = buf.readInt
    value = buf.readInt
  }

  override def toBytes(buf: ByteBuf) {
    buf.writeInt(index)
    buf.writeInt(value)
  }

  override def onMessage(message: MessageContainerUpdate, ctx: MessageContext) = {
    Minecraft.getMinecraft.thePlayer.openContainer.updateProgressBar(message.index, message.value)
    null
  }
}
