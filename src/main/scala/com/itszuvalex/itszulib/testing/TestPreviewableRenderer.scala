package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.api.IPreviewableRenderer
import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.render.RenderUtils._
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack

/**
  * Created by Christopher Harris (Itszuvalex) on 8/26/15.
  */
class TestPreviewableRenderer extends IPreviewableRenderer {
  /**
    * Coordinates are the location to render at.  This is usually the facing off-set location that, if the player right-clicked, a block would be placed at.
    *
    * @param stack ItemStack of IPreviewable Item
    * @param world World
    * @param x     X Location
    * @param y     Y Location
    * @param z     Z Location
    */
  override def renderAtLocation(stack: ItemStack, loc: Loc4, rx: Double, ry: Double, rz: Double): Unit = {
    renderCube(rx.toFloat, ry.toFloat, rz.toFloat, 0, 0, 0, 1, 1, 1, getDefaultTextureForBlock(Blocks.DIAMOND_ORE))
  }
}
