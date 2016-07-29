package com.itszuvalex.itszulib.network.messages

import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler}

/**
  * Created by Christopher Harris (Itszuvalex) on 7/29/16.
  */
abstract class MessageBase[REQ <: IMessage, REP <: IMessage] extends IMessage with IMessageHandler[REQ, REP]
