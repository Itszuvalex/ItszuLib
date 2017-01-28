package com.itszuvalex.itszulib.network.messages

import com.itszuvalex.itszulib.container.ContainerBase
import com.itszuvalex.itszulib.container.sync.ISync
import com.itszuvalex.itszulib.util.Debug
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, MessageContext}
import org.apache.logging.log4j.Level

/**
  * Created by Chris on 12/13/2016.
  */
object MessageSync {
  val INDEX_KEY = "index"
  val NBT_KEY   = "nbt"
  val GUI_KEY   = "gui"
}

class MessageSync(sync: ISync[_]) extends MessageUpdateNBT[MessageSync, IMessage]({
  val nbt = new NBTTagCompound
  nbt.setInteger(MessageSync.GUI_KEY, Option(sync).map(_.GuiID).getOrElse(0))
  nbt.setInteger(MessageSync.INDEX_KEY, Option(sync).map(_.syncIndex).getOrElse(0))
  nbt.setTag(MessageSync.NBT_KEY, Option(sync).map(_.writeNBT()).orNull)
  nbt
}) {

  def this() = this(null)

  override def onMessage(message: MessageSync, ctx: MessageContext): IMessage = {
    Minecraft.getMinecraft.thePlayer.openContainer match {
      case a: ContainerBase if a.GuiID == message.nbt.getInteger(MessageSync.GUI_KEY) => a.getSync(message.nbt.getInteger(MessageSync.INDEX_KEY)).handleNBT(message.nbt.getTag(MessageSync.NBT_KEY))
        Debug.log(Level.WARN, "Received Sync for index:" + message.nbt.getInteger(MessageSync.INDEX_KEY) + "item:" + a.getSync(message.nbt.getInteger(MessageSync.INDEX_KEY)).value.toString)
      case _ =>
    }
    null
  }
}
