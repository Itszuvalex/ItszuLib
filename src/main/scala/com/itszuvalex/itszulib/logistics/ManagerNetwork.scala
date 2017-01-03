package com.itszuvalex.itszulib.logistics

import java.util.concurrent.ConcurrentHashMap

import com.itszuvalex.itszulib.ItszuLib
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.NetworkRegistry

import scala.actors.threadpool.AtomicInteger
import scala.collection.JavaConverters._

/**
  * Created by Christopher on 4/5/2015.
  */
object ManagerNetwork {
  val INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(ItszuLib.ID.toLowerCase + "|" + "logistics")

  private val nextID     = new AtomicInteger(0)
  private val networkMap = new ConcurrentHashMap[Int, INetwork[_, _]].asScala

  def getNextID = nextID.getAndIncrement

  def addNetwork(network: INetwork[_, _]) = networkMap(network.ID) = network

  def removeNetwork(network: INetwork[_, _]) = networkMap.remove(network.ID)

  def init(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)
  }

  def getNetwork(id: Int) = networkMap.get(id)

  @SubscribeEvent def onTickBegin(event: TickEvent.ServerTickEvent): Unit = {
    if (event.phase == TickEvent.Phase.START) networkMap.values.foreach(_.onTickStart())
    if (event.phase == TickEvent.Phase.END) networkMap.values.foreach(_.onTickEnd())
  }

  @SubscribeEvent def onWorldUnload(event: WorldEvent.Unload): Unit = {
    if (!FMLCommonHandler.instance().getMinecraftServerInstance.isServerRunning) {
      networkMap.clear()
    }
  }
}
