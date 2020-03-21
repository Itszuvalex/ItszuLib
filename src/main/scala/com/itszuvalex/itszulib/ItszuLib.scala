package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.ManagerCapabilities
import com.itszuvalex.itszulib.initialization.{InitializationStage, ModInit}
import com.itszuvalex.itszulib.logistics.ManagerNetwork
import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import com.itszuvalex.itszulib.proxy.ProxyCommon
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInterModComms
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.{Mod, SidedProxy}
import org.apache.logging.log4j.LogManager

/**
  * Created by Christopher on 4/5/2015.
  */
@Mod(modid = ItszuLib.ID, name = ItszuLib.NAME, version = ItszuLib.VERSION, modLanguage = "scala")
object ItszuLib extends ModInit {
  final val NAME    = "ItszuLib"
  final val ID      = "itszulib"
  final val VERSION = Version.FULL_VERSION
  final val logger  = LogManager.getLogger(ID)

  final val CLIENT_PROXY = "com.itszuvalex.itszulib.proxy.ProxyClient";
  final val SERVER_PROXY = "com.itszuvalex.itszulib.proxy.ProxyServer";

  @SidedProxy(clientSide = ItszuLib.CLIENT_PROXY, serverSide = ItszuLib.SERVER_PROXY)
  var proxy: ProxyCommon = null

  initializationManager.addInitStage(InitializationStage.Pre, () => ItszuLibPacketHandler.init())
  initializationManager.addInitStage(InitializationStage.Pre, () => NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy))
  initializationManager.addInitStage(InitializationStage.Pre, () => ManagerCapabilities.register())
  initializationManager.addInitStage(InitializationStage.Pre, () => ManagerNetwork.init())
  initializationManager.addInitStage(InitializationStage.Main, () => proxy.init())
  initializationManager.addInitializable(ItszuItems)
  initializationManager.addInitializable(ItszuBlocks)

  @EventHandler def imcCallback(event: FMLInterModComms.IMCEvent) {
    InterModComms.imcCallback(event)
  }
}
