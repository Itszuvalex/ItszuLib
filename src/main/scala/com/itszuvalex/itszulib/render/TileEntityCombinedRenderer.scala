package com.itszuvalex.itszulib.render

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity

/**
  * Created by Chris on 8/14/2016.
  */
abstract class TileEntityCombinedRenderer[T <: TileEntity] extends TileEntitySpecialRenderer[T] {
  override def renderTileEntityAt(te: T, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int): Unit = {
    if (te == null && destroyStage == -1)
      renderTileEntityAsItem(x, y, z, partialTicks, destroyStage)
    else
      renderTileEntityInWorld(te, x, y, z, partialTicks, destroyStage)
  }

  def renderTileEntityAsItem(x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int): Unit = {
    super.renderTileEntityAt(null.asInstanceOf[T], x, y, z, partialTicks, destroyStage)
  }

  def renderTileEntityInWorld(te: T, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int): Unit = {
    super.renderTileEntityAt(te, x, y, z, partialTicks, destroyStage)
  }
}
