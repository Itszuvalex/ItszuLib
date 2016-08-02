package com.itszuvalex.itszulib.network

import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler}
import net.minecraftforge.fml.relauncher.Side

/**
  * Created by Christopher Harris (Itszuvalex) on 7/29/16.
  */
abstract class PacketHandler(val channel: String) {
  val INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(channel)

  var nextIdentifier: Int = 0

  def getIdentifier: Int = {
    if (nextIdentifier > (Byte.MaxValue.toInt & 0x000F)) {
      throw new RuntimeException("Ran out of Packet Identifiers for channel:" + channel)
    }

    val ret = nextIdentifier
    nextIdentifier += 1
    ret
  }

  def register[HANDLER <: IMessageHandler[HANDLER, IMessage] with IMessage](clazz: Class[HANDLER], receiver: Side): Unit = {
    INSTANCE.registerMessage[HANDLER, IMessage](clazz, clazz, getIdentifier, receiver)
  }

}
