package com.itszuvalex.itszulib.api

import com.itszuvalex.itszulib.util.PlayerUtils
import net.minecraft.item.Item
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

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

  def snapToBlockGrid: Boolean = true

}
