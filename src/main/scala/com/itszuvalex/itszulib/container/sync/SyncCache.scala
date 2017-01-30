package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.container.ContainerBase
import com.itszuvalex.itszulib.network.messages.MessageSync
import net.minecraftforge.event.entity.player.PlayerContainerEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by Chris on 1/29/2017.
  */
class SyncCache {
  val syncCache: mutable.HashMap[Int, ArrayBuffer[MessageSync]] = mutable.HashMap[Int, ArrayBuffer[MessageSync]]()

  def cache(messageSync: MessageSync): Unit = {
    syncCache.getOrElseUpdate(messageSync.GuiId, new ArrayBuffer[MessageSync]()) += messageSync
  }

  @SubscribeEvent
  def onContainerOpen(event: PlayerContainerEvent.Open): Unit = {
    event.getContainer match {
      case c: ContainerBase =>
        syncCache.get(c.GuiID).foreach { a =>
          a.foreach { message =>
            val sync = c.getSync(message.SyncIndex)
            if (sync.state < message.State) {
              sync.handleNBT(message.NBT)
              sync.updateState(message.State)
            }
          }
        }
      case _ =>
    }
    syncCache.clear()
  }
}
