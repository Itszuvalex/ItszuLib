package com.itszuvalex.itszulib.render

import com.itszuvalex.itszulib.render.RenderUtils.translationBlock
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{EnumFacing, ResourceLocation}

/**
  * Created by Chris on 7/31/2016.
  */
abstract class TileEntityRenderCube[T <: TileEntity](modName: String, val sides: Array[ResourceLocation], texName: String) extends TileEntitySpecialRenderer[T] {

  def this(modName: String, texName: String) = this(modName, new Array[ResourceLocation](6), texName)

  def this(modName: String, tex: ResourceLocation) = this(modName, Array.fill[ResourceLocation](6)(tex), "")

  sides.indices.view.filter(sides(_) == null).foreach { i =>
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
