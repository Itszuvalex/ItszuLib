package com.itszuvalex.itszulib.render

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

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
    //TODO: Different transforms for GUI (current), first person holding and third person holding
    GL11.glScalef(.615f, .615f, .615f)
    GL11.glTranslatef(1.525f, .375f, 0)
    GL11.glRotatef(-135, 0, 1, 0)
    GL11.glRotatef(-30, 1, 0, 1)
  }

  def renderTileEntityInWorld(te: T, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int): Unit = {
    super.renderTileEntityAt(te, x, y, z, partialTicks, destroyStage)
  }
}
