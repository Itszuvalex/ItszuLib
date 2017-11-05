package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.ManagerCapabilities
import com.itszuvalex.itszulib.logistics.ManagerNetwork
import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import com.itszuvalex.itszulib.proxy.ProxyCommon
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLInterModComms, FMLPostInitializationEvent, FMLPreInitializationEvent}
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.{Mod, SidedProxy}
import org.apache.logging.log4j.LogManager

/**
  * Created by Christopher on 4/5/2015.
  */
@Mod(modid = ItszuLib.ID, name = "ItszuLib", version = ItszuLib.VERSION, modLanguage = "scala")
object ItszuLib {
  final val ID      = "itszulib"
  final val VERSION = Version.FULL_VERSION
  final val logger  = LogManager.getLogger(ID)


  @SidedProxy(clientSide = "com.itszuvalex.itszulib.proxy.ProxyClient",
    serverSide = "com.itszuvalex.itszulib.proxy.ProxyServer")
  var proxy: ProxyCommon = null

  @EventHandler def preInit(event: FMLPreInitializationEvent): Unit = {
    ItszuLibPacketHandler.init()
    //    PlayerUUIDTracker.init()
    //    PlayerUUIDTracker.setFile(new File())
    NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy)
    MinecraftForge.EVENT_BUS.register(ItszuBlocks)
    MinecraftForge.EVENT_BUS.register(ItszuItems)

    ManagerCapabilities.register()
    ManagerNetwork.init()
  }

  @EventHandler def load(event: FMLInitializationEvent): Unit = {
    proxy.init()
  }

  @EventHandler def postInit(event: FMLPostInitializationEvent): Unit = {

  }

  @EventHandler def imcCallback(event: FMLInterModComms.IMCEvent) {
    InterModComms.imcCallback(event)
  }

}
