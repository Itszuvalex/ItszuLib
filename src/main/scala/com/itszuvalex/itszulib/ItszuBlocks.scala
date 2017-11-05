package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.testing.{BlockInventoryTest, BlockLocTrackerTest, BlockPortalTest, BlockTankTest}
import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ItszuBlocks {
  var blockTankTest  : Block = _
  var blockPortalTest: Block = _
  var blockInvTest   : Block = _

  @SubscribeEvent
  def registerBlocks(event: RegistryEvent.Register[Block]): Unit = {
    event.getRegistry.register(new BlockLocTrackerTest().setCreativeTab(CreativeTabs.BUILDING_BLOCKS))
    blockTankTest = new BlockTankTest().setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setRegistryName(ItszuLib.ID.toLowerCase(), "BlockTankTest").setUnlocalizedName("BlockTankTest")
    blockInvTest = new BlockInventoryTest().setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setRegistryName(ItszuLib.ID.toLowerCase(), "BlockInventoryTest").setUnlocalizedName("BlockInventoryTest")
    blockPortalTest = new BlockPortalTest().setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setRegistryName(ItszuLib.ID.toLowerCase(), "BlockPortalTest").setUnlocalizedName("BlockPortalTest")
    event.getRegistry.register(blockTankTest)
    event.getRegistry.register(blockInvTest)
    event.getRegistry.register(blockPortalTest)
  }
}
