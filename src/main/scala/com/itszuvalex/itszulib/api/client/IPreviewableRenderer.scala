package com.itszuvalex.itszulib.api.client

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.wrappers.IItemStack
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

/**
  * Created by Christopher Harris (Itszuvalex) on 8/26/15.
  */
@SideOnly(Side.CLIENT)
trait IPreviewableRenderer {

  /**
    * Coordinates are the location to render at.  This is usually the facing off-set location that, if the player right-clicked, a block would be placed at.
    *
    * @param stack ItemStack of IPreviewable Item
    * @param loc   Loc
    * @param rx    X Render location
    * @param ry    Y Render location
    * @param rz    Z Render location
    */
  def renderAtLocation(stack: IItemStack, player: EntityPlayer, loc: Loc4, rx: Double, ry: Double, rz: Double): Unit

  def render(stack: IItemStack, player: EntityPlayer)

}
