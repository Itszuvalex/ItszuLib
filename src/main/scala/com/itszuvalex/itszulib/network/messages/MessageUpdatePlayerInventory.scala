package com.itszuvalex.itszulib.network.messages

import com.itszuvalex.itszulib.api.wrappers.IItemStack
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, MessageContext}

/**
  * Created by Christopher Harris (Itszuvalex) on 7/29/16.
  */
class MessageUpdatePlayerInventory(nbt: NBTTagCompound) extends MessageUpdateNBT[MessageUpdatePlayerInventory, IMessage](nbt) {
  def this() = this(null)

  override def onMessage(message: MessageUpdatePlayerInventory, ctx: MessageContext): IMessage = {
    Minecraft.getMinecraft.addScheduledTask(new Runnable {
      override def run(): Unit = Minecraft.getMinecraft.player.inventory.setItemStack(IItemStack.Serializer.deserialize(message.nbt).toMinecraft)
    })
    null
  }
}
