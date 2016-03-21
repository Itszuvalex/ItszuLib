package com.itszuvalex.itszulib.api

import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.item.ItemStack
import net.minecraft.world.World

/**
  * Created by Christopher Harris (Itszuvalex) on 8/26/15.
  */
@SideOnly(Side.CLIENT)
trait IPreviewableRenderer {

  /**
    * Coordinates are the location to render at.  This is usually the facing off-set location that, if the player right-clicked, a block would be placed at.
    *
    * @param stack ItemStack of IPreviewable Item
    * @param world World
    * @param x     X Location
    * @param y     Y Location
    * @param z     Z Location
    * @param rx    X Render location
    * @param ry    Y Render location
    * @param rz    Z Render location
    */
  def renderAtLocation(stack: ItemStack, world: World, x: Int, y: Int, z: Int,
                       rx: Double, ry: Double, rz: Double): Unit

}
