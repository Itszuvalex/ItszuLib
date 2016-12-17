package com.itszuvalex.itszulib.container

import com.itszuvalex.itszulib.container.sync.ISync
import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import com.itszuvalex.itszulib.network.messages.MessageContainerUpdate
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.{Container, IContainerListener}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
  * Created by Chris on 8/29/2014.
  */
abstract class ContainerBase extends Container {
  val syncs: ArrayBuffer[ISync[_]] = new ArrayBuffer[ISync[_]]

  def addSync(sync: ISync[_]): Unit = {
    sync.index = syncs.size
    syncs += sync
  }

  def getSync(index: Int) = syncs(index)

  override def detectAndSendChanges(): Unit = {
    super.detectAndSendChanges()
    syncs.filter(_.update()).foreach { sync =>
      listeners.foreach {
        case p: EntityPlayerMP => sync.sync(p)
        case _ =>
      }
    }
  }

  protected def sendUpdateToListener(container: Container, crafter: IContainerListener, index: Int, value: Int) {
    crafter match {
      case p: EntityPlayerMP =>
        ItszuLibPacketHandler.INSTANCE.sendTo(new MessageContainerUpdate(index, value), p)
      case _ =>
        crafter.sendProgressBarUpdate(container, index, value)
    }
  }
}
