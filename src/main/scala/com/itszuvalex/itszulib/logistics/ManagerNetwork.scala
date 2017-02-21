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
  val INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(ItszuLib.ID.toLowerCase + "|" + "logistics")

  private val nextID     = new AtomicInteger(0)
  private val networkMap = new ConcurrentHashMap[Int, INetwork[_, _]].asScala

  def getNextID = nextID.getAndIncrement

  def addNetwork(network: INetwork[_, _]) = {
    Debug.log(Level.WARN, "Added Network:" + network.ID)
    Debug.log(Level.WARN, "Active Networks:" + networkMap.size)
    networkMap(network.ID) = network
    Debug.log(Level.WARN, "Active Network After Addition:" + networkMap.size)
  }

  def removeNetwork(network: INetwork[_, _]) = {
    Debug.log(Level.WARN, "Removed Network:" + network.ID)
    Debug.log(Level.WARN, "Active Networks:" + networkMap.size)
    networkMap.remove(network.ID)
    Debug.log(Level.WARN, "Active Networks After Removal:" + networkMap.size)
  }

  def init(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)
  }

  def getNetwork(id: Int) = networkMap.get(id)

  @SubscribeEvent def onTickBegin(event: TickEvent.ServerTickEvent): Unit = {
    if (event.phase == TickEvent.Phase.START) networkMap.values.foreach(_.onTickStart())
    if (event.phase == TickEvent.Phase.END) networkMap.values.foreach(_.onTickEnd())
  }

  @SubscribeEvent def onWorldUnload(event: WorldEvent.Unload): Unit = {
    val server = FMLCommonHandler.instance().getMinecraftServerInstance
    if (server == null || !server.isServerRunning) {
      networkMap.clear()
    }
  }
}
