package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.testing.{BlockLocTrackerTest, BlockPortalTest}
import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ItszuBlocks {
  var blockPortalTest: Block = _

  @SubscribeEvent
  def registerBlocks(event: RegistryEvent.Register[Block]): Unit = {
    event.getRegistry.register(new BlockLocTrackerTest().setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setRegistryName(ItszuLib.ID.toLowerCase(), "BlockLocTrackerTest").setUnlocalizedName("BlockLocTrackerTest"))
    blockPortalTest = new BlockPortalTest().setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setRegistryName(ItszuLib.ID.toLowerCase(), "BlockPortalTest").setUnlocalizedName("BlockPortalTest")
    event.getRegistry.register(blockPortalTest)
  }
}
