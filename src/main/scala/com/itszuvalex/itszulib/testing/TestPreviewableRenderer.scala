package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.api.client.IPreviewableRenderer
import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.wrappers.IItemStack
import com.itszuvalex.itszulib.render.RenderUtils._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks

/**
  * Created by Christopher Harris (Itszuvalex) on 8/26/15.
  */
class TestPreviewableRenderer extends IPreviewableRenderer {
  /**
    * Coordinates are the location to render at.  This is usually the facing off-set location that, if the player right-clicked, a block would be placed at.
    *
    * @param stack  IItemStack of IPreviewable Item
    * @param player EntityPlayer
    * @param rx     render X Location
    * @param ry     render Y Location
    * @param rz     render Z Location
    */
  override def renderAtLocation(stack: IItemStack, player: EntityPlayer, loc: Loc4, rx: Double, ry: Double, rz: Double): Unit = {
    renderCube(rx.toFloat - .5f, ry.toFloat, rz.toFloat - .5f, 0, 0, 0, 1, 1, 1, getDefaultTextureForBlock(Blocks.DIAMOND_ORE))
  }

  override def render(stack: IItemStack, player: EntityPlayer): Unit = {}
}
