package com.itszuvalex.itszulib.container

import com.itszuvalex.itszulib.network.PacketHandler
import com.itszuvalex.itszulib.network.messages.MessageContainerUpdate
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.{Container, ICrafting}

/**
  * Created by Chris on 8/29/2014.
  */
abstract class ContainerBase extends Container {
  protected def sendUpdateToCrafter(container: Container, crafter: ICrafting, index: Int, value: Int) {
    crafter match {
      case p: EntityPlayerMP =>
        PacketHandler.INSTANCE.sendTo(new MessageContainerUpdate(index, value), p)
      case _ =>
        crafter.sendProgressBarUpdate(container, index, value)
    }
  }
}
