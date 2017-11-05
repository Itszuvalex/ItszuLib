package com.itszuvalex.itszulib.render

import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity

/**
  * Created by Chris on 8/14/2016.
  */
abstract class TileEntityCombinedRenderer[T <: TileEntity] extends TileEntitySpecialRenderer[T] {
  override def renderTileEntityFast(te: T, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, partial: Float, buffer: BufferBuilder) = {
    if (te == null && destroyStage == -1)
      renderTileEntityAsItem(x, y, z, partialTicks)
    else
      renderTileEntityInWorld(te, x, y, z, partialTicks, destroyStage, partial, buffer)
  }

  def renderTileEntityAsItem(x: Double, y: Double, z: Double, partialTicks: Float): Unit = {

  }

  def renderTileEntityInWorld(te: T, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, partial: Float, buffer: BufferBuilder): Unit = {
    super.renderTileEntityFast(te, x, y, z, partialTicks, destroyStage, partial, buffer)
  }
}
