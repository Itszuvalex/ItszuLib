package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.testing.ItemPreviewable
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.{Item, ItemBlock}
import net.minecraft.util.ResourceLocation
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ItszuItems {

  @SubscribeEvent
  def registerItems(event: RegistryEvent.Register[Item]): Unit = {
    event.getRegistry.register(new ItemBlock(ItszuBlocks.blockTankTest).setRegistryName(ItszuBlocks.blockTankTest.getRegistryName))
    event.getRegistry.register(new ItemBlock(ItszuBlocks.blockInvTest).setRegistryName(ItszuBlocks.blockInvTest.getRegistryName))
    event.getRegistry.register(new ItemBlock(ItszuBlocks.blockPortalTest).setRegistryName(ItszuBlocks.blockPortalTest.getRegistryName))
    val prev = new ItemPreviewable().setRegistryName(new ResourceLocation("TilePreviewable"))
    prev.setCreativeTab(CreativeTabs.DECORATIONS)
    event.getRegistry.register(prev)
  }
}
