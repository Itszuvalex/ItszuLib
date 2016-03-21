package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.network.PacketHandler
import com.itszuvalex.itszulib.proxy.ProxyCommon
import com.itszuvalex.itszulib.testing.{BlockLocTrackerTest, BlockPortalTest, BlockTankTest, ItemPreviewable}
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLInterModComms, FMLPostInitializationEvent, FMLPreInitializationEvent}
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.common.{Mod, SidedProxy}
import net.minecraft.creativetab.CreativeTabs
import org.apache.logging.log4j.LogManager

/**
  * Created by Christopher on 4/5/2015.
  */
@Mod(modid = ItszuLib.ID, name = ItszuLib.ID, version = ItszuLib.VERSION, modLanguage = "scala")
object ItszuLib {
  final val ID      = "ItszuLib"
  final val VERSION = Version.FULL_VERSION
  final val logger  = LogManager.getLogger(ID)

  @SidedProxy(clientSide = "com.itszuvalex.itszulib.proxy.ProxyClient",
              serverSide = "com.itszuvalex.itszulib.proxy.ProxyServer")
  var proxy: ProxyCommon = null

  @EventHandler def preInit(event: FMLPreInitializationEvent): Unit = {
    PacketHandler.init()
    //    PlayerUUIDTracker.init()
    //    PlayerUUIDTracker.setFile(new File())
    proxy.init()
    NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy)
  }

  @EventHandler def load(event: FMLInitializationEvent): Unit = {
    GameRegistry.registerBlock(new BlockPortalTest, "BlockPortalTest").setCreativeTab(CreativeTabs.tabBlock)
    GameRegistry.registerBlock(new BlockLocTrackerTest, "BlockLocTrackerTest").setCreativeTab(CreativeTabs.tabBlock)
    GameRegistry.registerBlock(new BlockTankTest, "BlockTankTest").setCreativeTab(CreativeTabs.tabBlock)
    val prev = new ItemPreviewable
    prev.setCreativeTab(CreativeTabs.tabDecorations)
    GameRegistry.registerItem(prev, "TilePreviewable")
  }

  @EventHandler def postInit(event: FMLPostInitializationEvent): Unit = {

  }

  @EventHandler def imcCallback(event: FMLInterModComms.IMCEvent) {
    InterModComms.imcCallback(event)
  }

}
