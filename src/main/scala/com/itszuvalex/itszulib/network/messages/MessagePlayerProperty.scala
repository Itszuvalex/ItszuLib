package com.itszuvalex.itszulib.network.messages

import java.io.{ByteArrayInputStream, IOException}

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.util.PlayerUtils
import com.itszuvalex.itszulib.player.PlayerProperties
import cpw.mods.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}
import io.netty.buffer.ByteBuf
import net.minecraft.nbt.{CompressedStreamTools, NBTTagCompound}
import org.apache.logging.log4j.Level

/**
 * Created by Christopher Harris (Itszuvalex) on 10/22/14.
 */
class MessagePlayerProperty(private var username: String, private var data: NBTTagCompound) extends IMessage with IMessageHandler[MessagePlayerProperty, IMessage] {
  def this() = this(null, null)

  override def fromBytes(buf: ByteBuf) = {
    val slength = buf.readInt()
    val name = new Array[Byte](slength)
    buf.readBytes(name)
    username = new String(name)
    val length = buf.readInt()
    val bytes = new Array[Byte](length)
    buf.readBytes(bytes)
    try {
      data = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes))
    }
    catch {
      case e: IOException =>
        e.printStackTrace()
        ItszuLib.logger.log(Level.ERROR, "Error decompressing player data.")
    }
  }

  override def toBytes(buf: ByteBuf): Unit = {
    var bytes: Array[Byte] = null
    try {
      bytes = CompressedStreamTools.compress(data)
    }
    catch {
      case e: IOException =>
        e.printStackTrace()
        ItszuLib.logger.log(Level.ERROR, "Error compressing player data.")
        return
    }
    buf.writeInt(username.getBytes.length)
    buf.writeBytes(username.getBytes)
    buf.writeInt(bytes.length)
    buf.writeBytes(bytes)
  }

  override def onMessage(message: MessagePlayerProperty, ctx: MessageContext): IMessage = {
    val player = PlayerUtils.getLocalPlayer(message.username)
    if (player != null)
      PlayerProperties.get(player).handlePacket(message.data)
    null
  }
}
