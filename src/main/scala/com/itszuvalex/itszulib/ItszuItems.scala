package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.initialization.{IInitializable, ItemBuilder}
import com.itszuvalex.itszulib.testing.ItemPreviewable
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation

object ItszuItems extends IInitializable {
  var ITEM_PREVIEWABLE: Item = _

  override def preInit(): Unit = {
    ItszuLib.initializationManager.addItemBuilder[ItemPreviewable](
      new ItemBuilder().setFactory(() => new ItemPreviewable()).setCreativeTab(CreativeTabs.DECORATIONS).setRegistryName(new ResourceLocation("TilePreviewable")),
      ITEM_PREVIEWABLE = _)
  }

  /*
    event.getRegistry.register(new ItemBlock(ItszuBlocks.blockPortalTest).setRegistryName(ItszuBlocks.blockPortalTest.getRegistryName))

   */
}
