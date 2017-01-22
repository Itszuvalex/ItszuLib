package com.itszuvalex.itszulib.render

import com.itszuvalex.itszulib.render.RenderUtils.translationBlock
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{EnumFacing, ResourceLocation}
import org.lwjgl.opengl.GL11

/**
  * Created by Chris on 7/31/2016.
  */
abstract class TileEntityRenderCube[T <: TileEntity](modName: String, val sides: Array[ResourceLocation], texName: String) extends TileEntityCombinedRenderer[T] {
  var facing = EnumFacing.NORTH

  def this(modName: String, texName: String) = this(modName, new Array[ResourceLocation](6), texName)

  def this(modName: String, tex: ResourceLocation) = this(modName, Array.fill[ResourceLocation](6)(tex), "")

  sides.indices.view.filter(sides(_) == null).foreach { i =>
    sides(i) = new ResourceLocation(modName, "textures/blocks/" + texName + "_" + EnumFacing.values()(i).toString + ".png")
  }

  override def renderTileEntityAsItem(x: Double, y: Double, z: Double, partialTicks: Float): Unit = {
    super.renderTileEntityAsItem(x, y, z, partialTicks)
    translationBlock(x, y, z) {
      renderCube()
    }
  }

  override def renderTileEntityInWorld(te: T, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int): Unit = {
    super.renderTileEntityInWorld(te, x, y, z, partialTicks, destroyStage)
    translationBlock(x, y, z) {
      renderCube()
    }
  }

  def renderCube() = {
    preRender()
    sides.indices.foreach { i =>
      val facing = EnumFacing.values()(i)
      preFaceRender(facing)
      RenderUtils.drawArbitraryFace(0, 0, 0, 0, 1, 0, 1, 0, 1, facing, null, 0, 1, 0, 1)
      postFaceRender(facing)
    }
    postRender()
  }

  def preRender(): Unit = {
    GL11.glPushMatrix()
    translationBlock(.5, .5, .5) {
      GL11.glRotatef(AngleFromFacing, 0, 1, 0)
    }
  }

  def postRender(): Unit = {
    GL11.glPopMatrix()
  }

  def preFaceRender(facing: EnumFacing): Unit = {
    bindTexture(sides(facing.getIndex))
  }

  def postFaceRender(facing: EnumFacing): Unit = {

  }

  protected def AngleFromFacing = {
    facing match {
      case EnumFacing.NORTH => 0
      case EnumFacing.SOUTH => 180
      case EnumFacing.EAST => 270
      case EnumFacing.WEST => 90
      case _ => 0
    }
  }

}
