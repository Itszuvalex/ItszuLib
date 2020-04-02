package com.itszuvalex.itszulib.logistics

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.util.Debug
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import org.apache.logging.log4j.Level

import scala.collection.JavaConverters._

/**
  * Created by Christopher on 4/5/2015.
  */
object ManagerNetwork {
  val NETWORK_CHANNEL_INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(ItszuLib.ID.toLowerCase + "|" + "logistics")
  private var INSTANCE: INetworkManager = new ManagerNetwork()

  def instance: INetworkManager = INSTANCE

  def init(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)
  }

  def setInstance(instance: INetworkManager): Unit = INSTANCE = instance

  @SubscribeEvent def onTickBegin(event: TickEvent.ServerTickEvent): Unit = {
    event.phase match {
      case TickEvent.Phase.START => INSTANCE.onTickStart()
      case TickEvent.Phase.END => INSTANCE.onTickEnd()
      case _ =>
    }
  }

  @SubscribeEvent def onWorldUnload(event: WorldEvent.Unload): Unit = {
    val server = FMLCommonHandler.instance().getMinecraftServerInstance
    if (server == null || !server.isServerRunning) {
      INSTANCE.clear()
    }
  }
}

class ManagerNetwork extends INetworkManager {
  private val nextID     = new AtomicInteger(0)
  private val networkMap = new ConcurrentHashMap[Int, INetwork[_, _]].asScala

  override def getNextID: Int = nextID.getAndIncrement

  override def addNetwork(network: INetwork[_, _]): Unit = {
    Debug.log(Level.INFO, "Added Network:" + network.ID)
    Debug.log(Level.INFO, "Active Networks:" + networkMap.size)
    networkMap(network.ID) = network
    Debug.log(Level.INFO, "Active Network After Addition:" + networkMap.size)
  }

  override def removeNetwork(network: INetwork[_, _]): Unit = {
    Debug.log(Level.INFO, "Removed Network:" + network.ID)
    Debug.log(Level.INFO, "Active Networks:" + networkMap.size)
    networkMap.remove(network.ID)
    Debug.log(Level.INFO, "Active Networks After Removal:" + networkMap.size)
  }

  override def getNetwork(id: Int): Option[INetwork[_, _]] = networkMap.get(id)

  override def onTickStart(): Unit = networkMap.values.foreach(_.onTickStart())

  override def onTickEnd(): Unit = networkMap.values.foreach(_.onTickEnd())

  override def clear(): Unit = networkMap.clear()

  override def networkCount: Int = networkMap.size

  override def networks: Iterable[INetwork[_, _]] = networkMap.values

}
