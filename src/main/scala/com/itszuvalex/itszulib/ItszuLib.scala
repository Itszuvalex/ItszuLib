package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.ManagerCapabilities
import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import com.itszuvalex.itszulib.proxy.ProxyCommon
import com.itszuvalex.itszulib.testing.{BlockLocTrackerTest, BlockPortalTest, BlockTankTest, ItemPreviewable}
import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemBlock
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLInterModComms, FMLPostInitializationEvent, FMLPreInitializationEvent}
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.registry.GameRegistry
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

  var blockTankTest  : Block = _
  var blockPortalTest: Block = _

  @SidedProxy(clientSide = "com.itszuvalex.itszulib.proxy.ProxyClient",
    serverSide = "com.itszuvalex.itszulib.proxy.ProxyServer")
  var proxy: ProxyCommon = null

  @EventHandler def preInit(event: FMLPreInitializationEvent): Unit = {
    ItszuLibPacketHandler.init()
    //    PlayerUUIDTracker.init()
    //    PlayerUUIDTracker.setFile(new File())
    NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy)

    ManagerCapabilities.register()
  }

  @EventHandler def load(event: FMLInitializationEvent): Unit = {

    //GameRegistry.registerBlock(new BlockPortalTest, "BlockPortalTest").setCreativeTab(CreativeTabs.BUILDING_BLOCKS)
    GameRegistry.register(new BlockLocTrackerTest().setCreativeTab(CreativeTabs.BUILDING_BLOCKS), new ResourceLocation("BlockLocTrackerTest"))
    blockTankTest = new BlockTankTest().setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setRegistryName(ItszuLib.ID.toLowerCase(), "BlockTankTest").setUnlocalizedName("BlockTankTest")
    blockPortalTest = new BlockPortalTest().setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setRegistryName(ItszuLib.ID.toLowerCase(), "BlockPortalTest").setUnlocalizedName("BlockPortalTest")
    GameRegistry.register(blockTankTest)
    GameRegistry.register(blockPortalTest)
    GameRegistry.register(new ItemBlock(blockTankTest).setRegistryName(blockTankTest.getRegistryName).setUnlocalizedName("BlockTankTest"))
    GameRegistry.register(new ItemBlock(blockPortalTest).setRegistryName(blockPortalTest.getRegistryName).setUnlocalizedName("BlockPortalTest"))
    val prev = new ItemPreviewable
    prev.setCreativeTab(CreativeTabs.DECORATIONS)
    GameRegistry.register(prev, new ResourceLocation("TilePreviewable"))
    proxy.init()
  }

  @EventHandler def postInit(event: FMLPostInitializationEvent): Unit = {

  }

  @EventHandler def imcCallback(event: FMLInterModComms.IMCEvent) {
    InterModComms.imcCallback(event)
  }

}
