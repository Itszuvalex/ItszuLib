package com.itszuvalex.itszulib.render

import com.itszuvalex.itszulib.render.RenderUtils.translationBlock
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{EnumFacing, ResourceLocation}

/**
  * Created by Chris on 7/31/2016.
  */
abstract class TileEntityRenderCube[T <: TileEntity](modName: String, texName: String) extends TileEntitySpecialRenderer[T] {
  val sides = new Array[ResourceLocation](6)

  sides.indices.foreach { i =>
    sides(i) = new ResourceLocation(modName, "textures/blocks/" + texName + "_" + EnumFacing.values()(i).toString + ".png")
                        }

  override def renderTileEntityAt(te: T, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int): Unit = {
    translationBlock(x, y, z) {
                                renderCube()
                              }

  }

  def renderCube() = {
    sides.indices.foreach { i =>
      val facing = EnumFacing.values()(i)
      preRender(facing)
      RenderUtils.drawArbitraryFace(0, 0, 0, 0, 1, 0, 1, 0, 1, facing, null, 0, 1, 0, 1)
      postRender(facing)
                          }
  }

  def preRender(facing: EnumFacing): Unit = {
    bindTexture(sides(facing.getIndex))
  }

  def postRender(facing: EnumFacing): Unit = {

  }
}
