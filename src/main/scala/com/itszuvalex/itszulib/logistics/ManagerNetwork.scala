package com.itszuvalex.itszulib.logistics

import java.util.concurrent.ConcurrentHashMap

import com.itszuvalex.itszulib.ItszuLib
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.network.NetworkRegistry
import net.minecraftforge.common.MinecraftForge

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

}
