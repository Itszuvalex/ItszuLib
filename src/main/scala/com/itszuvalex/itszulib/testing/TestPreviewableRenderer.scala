package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.api.IPreviewableRenderer
import com.itszuvalex.itszulib.render.RenderUtils
import net.minecraft.client.renderer.Tessellator
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.world.World

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
  override def renderAtLocation(stack: ItemStack, world: World, x: Int, y: Int, z: Int,
                                rx: Double, ry: Double, rz: Double): Unit = {
    Tessellator.instance.startDrawingQuads()
    RenderUtils.renderCube(rx.toFloat, ry.toFloat, rz.toFloat, 0, 0, 0, 1, 1, 1, Blocks.diamond_ore.getIcon(0, 0))
    Tessellator.instance.draw()
  }
}
