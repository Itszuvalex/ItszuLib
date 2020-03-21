package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.initialization.{BlockBuilder, IInitializable}
import com.itszuvalex.itszulib.testing.{BlockLocTrackerTest, BlockPortalTest}
import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.util.ResourceLocation

object ItszuBlocks extends IInitializable {
  var blockPortalTest: Block = _

  override def preInit(): Unit = {
    ItszuLib.initializationManager.addBlockBuilder[BlockLocTrackerTest](
      new BlockBuilder().setFactory(() => new BlockLocTrackerTest()).setCreativeTab(CreativeTabs.BUILDING_BLOCKS)
                        .setRegistryName(new ResourceLocation(ItszuLib.ID.toLowerCase(), "BlockLocTrackerTest")).setUnlocalizedName("BlockLocTrackerTest"),
      _ => {})
    ItszuLib.initializationManager.addBlockBuilder[BlockPortalTest](
      new BlockBuilder().setFactory(() => new BlockPortalTest()).setCreativeTab(CreativeTabs.BUILDING_BLOCKS).setRegistryName(new ResourceLocation(ItszuLib.ID.toLowerCase(), "BlockPortalTest")).setUnlocalizedName("BlockPortalTest").setHasItemBlock(true),
      blockPortalTest = _)
  }
}
