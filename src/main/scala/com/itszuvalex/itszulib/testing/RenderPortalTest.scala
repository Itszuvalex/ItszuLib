package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.render.RenderUtils._
import com.itszuvalex.itszulib.render.{RenderUtils, ShaderUtils}
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.init.Blocks
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

/**
  * Created by Christopher on 4/11/2015.
  */
object RenderPortalTest {
  private val skyLocation    : ResourceLocation = new ResourceLocation("textures/environment/end_sky.png")
  private val pictureLocation: ResourceLocation = new ResourceLocation("textures/entity/end_portal.png")
}

class RenderPortalTest extends TileEntitySpecialRenderer[PortalTileTest] {

  override def render(te: PortalTileTest, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float): Unit = {
    renderBackground(x, y, z)

    //    bindTexture(RenderPortalTest.skyLocation)

    RenderUtils.bindBlockTextures()

    //    GL11.glPushMatrix()

    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
    //    GL11.glEnable(GL11.GL_BLEND)
    GL11.glDisable(GL11.GL_LIGHTING)
    //    GL11.glAlphaFunc(GL11.GL_ALWAYS, 1)
    //    GL11.glEnable(GL11.GL_DEPTH)
    //    GL11.glEnable(GL11.GL_DEPTH_TEST)
    val icon = RenderUtils.getDefaultTextureForBlock(Blocks.FIRE)
    //    drawBillboard(x + 0.5, y + 0.5, z + 0.5, 0, .5, icon.getMinU, icon.getMaxU, icon.getMinV, icon.getMaxV)
    GL11.glPopAttrib()
    //
    //    GL11.glPopMatrix()
  }

  def renderBackground(x: Double, y: Double, z: Double): Unit = {
    this.bindTexture(RenderPortalTest.skyLocation)
    ShaderUtils.bindShader(ShaderUtils.portal)
    addBoxVerts(x, y, z)
    ShaderUtils.releaseShader()
  }

  def addBoxVerts(x: Double, y: Double, z: Double): Unit = {
    val xmin = 0
    val xmax = 1
    val ymin = 0
    val ymax = 1
    val zmin = 0
    val zmax = 1
    translationBlock(x, y, z) {
      drawBlock(DefaultVertexFormats.POSITION) {
        addVertex(xmin, ymax, zmin).endVertex()
        addVertex(xmin, ymax, zmax).endVertex()
        addVertex(xmax, ymax, zmax).endVertex()
        addVertex(xmax, ymax, zmin).endVertex()

        addVertex(xmin, ymin, zmin).endVertex()
        addVertex(xmax, ymin, zmin).endVertex()
        addVertex(xmax, ymin, zmax).endVertex()
        addVertex(xmin, ymin, zmax).endVertex()

        addVertex(xmin, ymin, zmin).endVertex()
        addVertex(xmin, ymax, zmin).endVertex()
        addVertex(xmax, ymax, zmin).endVertex()
        addVertex(xmax, ymin, zmin).endVertex()

        addVertex(xmax, ymin, zmin).endVertex()
        addVertex(xmax, ymax, zmin).endVertex()
        addVertex(xmax, ymax, zmax).endVertex()
        addVertex(xmax, ymin, zmax).endVertex()

        addVertex(xmin, ymin, zmax).endVertex()
        addVertex(xmax, ymin, zmax).endVertex()
        addVertex(xmax, ymax, zmax).endVertex()
        addVertex(xmin, ymax, zmax).endVertex()

        addVertex(xmin, ymin, zmin).endVertex()
        addVertex(xmin, ymin, zmax).endVertex()
        addVertex(xmin, ymax, zmax).endVertex()
        addVertex(xmin, ymax, zmin).endVertex()
      }
    }
  }
}
