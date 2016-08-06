package com.itszuvalex.itszulib.network

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.network.messages._
import net.minecraftforge.fml.relauncher.Side

/**
  * Created by Christopher Harris (Itszuvalex) on 4/6/15.
  */
object ItszuLibPacketHandler extends PacketHandler(ItszuLib.ID.toLowerCase) {

  def init(): Unit = {
    register(classOf[MessageContainerUpdate], Side.CLIENT)
    register(classOf[MessageFluidSlotClick], Side.SERVER)
    register(classOf[MessageFluidTankUpdate], Side.CLIENT)
    register(classOf[MessageUpdatePlayerInventory], Side.CLIENT)
  }
}
