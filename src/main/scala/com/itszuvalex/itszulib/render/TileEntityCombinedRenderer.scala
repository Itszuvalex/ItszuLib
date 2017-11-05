package com.itszuvalex.itszulib.render

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity

/**
  * Created by Chris on 8/14/2016.
  */
abstract class TileEntityCombinedRenderer[T <: TileEntity] extends TileEntitySpecialRenderer[T] {
  override def render(te: T, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float) = {
    if (te == null && destroyStage == -1)
      renderTileEntityAsItem(x, y, z, partialTicks)
    else
      renderTileEntityInWorld(te, x, y, z, partialTicks, destroyStage, alpha)
  }

  def renderTileEntityAsItem(x: Double, y: Double, z: Double, partialTicks: Float): Unit = {

  }

  def renderTileEntityInWorld(te: T, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float): Unit = {
  }
}
