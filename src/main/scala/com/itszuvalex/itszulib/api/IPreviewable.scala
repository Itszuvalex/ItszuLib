package com.itszuvalex.itszulib.api

import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.item.Item

/**
  * Created by Christopher Harris (Itszuvalex) on 8/26/15.
  */
trait IPreviewable extends Item {

  /**
    *
    * @return The ID of IPreviewableRenderer.  This is separate from Forge RenderIDs.
    */
  @SideOnly(Side.CLIENT)
  def renderID: Int

}
