package com.itszuvalex.itszulib.container

import com.itszuvalex.itszulib.container.sync.ISync
import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import com.itszuvalex.itszulib.network.messages.MessageContainerUpdate
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.inventory.{Container, IContainerListener}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
  * Created by Chris on 8/29/2014.
  */
abstract class ContainerBase(val gui: Int, val registerSyncs: Boolean) extends Container {
  val syncs: ArrayBuffer[ISync[_]] = new ArrayBuffer[ISync[_]]

  def GuiID: Int = gui

  def addSync(sync: ISync[_]): Unit = {
    sync.syncIndex = syncs.size
    syncs += sync
  }

  def getSync(index: Int): ISync[_] = syncs(index)

  override def detectAndSendChanges(): Unit = {
    super.detectAndSendChanges()
    syncs.filter(_.update()).foreach { sync =>
      listeners.foreach {
        case p: EntityPlayerMP => sync.sync(p)
        case _ =>
      }
    }
  }


  override def addListener(listener: IContainerListener): Unit = {
    super.addListener(listener)
    listener match {
      case p: EntityPlayerMP => syncs.foreach(_.sync(p))
      case _ =>
    }
  }

  protected def sendUpdateToListener(container: Container, crafter: IContainerListener, index: Int, value: Int) {
    crafter match {
      case p: EntityPlayerMP =>
        ItszuLibPacketHandler.INSTANCE.sendTo(new MessageContainerUpdate(index, value), p)
      case _ =>
        crafter.sendWindowProperty(container, index, value)
    }
  }

  override def onContainerClosed(playerIn: EntityPlayer): Unit = {
    super.onContainerClosed(playerIn)
    syncs.foreach(_.clear())
  }
}
