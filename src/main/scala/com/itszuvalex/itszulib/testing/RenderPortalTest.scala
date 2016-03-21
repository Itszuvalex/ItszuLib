package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.render.{RenderUtils, ShaderUtils}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

/**
  * Created by Christopher on 4/11/2015.
  */
object RenderPortalTest {
  private val skyLocation    : ResourceLocation = new ResourceLocation("textures/environment/end_sky.png")
  private val pictureLocation: ResourceLocation = new ResourceLocation("textures/entity/end_portal.png")
}

class RenderPortalTest extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(p_147500_1_ : TileEntity, x: Double, y: Double, z: Double, p_147500_8_ : Float): Unit = {
    //    renderBackground(x, y, z)

    //    bindTexture(RenderPortalTest.skyLocation)

    val man = Minecraft.getMinecraft.getTextureManager
    man.bindTexture(man.getResourceLocation(0))

    //    GL11.glPushMatrix()

    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
    //    GL11.glEnable(GL11.GL_BLEND)
    GL11.glDisable(GL11.GL_LIGHTING)
    //    GL11.glAlphaFunc(GL11.GL_ALWAYS, 1)
    //    GL11.glEnable(GL11.GL_DEPTH)
    //    GL11.glEnable(GL11.GL_DEPTH_TEST)
    val icon = Blocks.fire.getFireIcon(0)
    RenderUtils.drawBillboard(x + 0.5, y + 0.5, z + 0.5, 0, .5, icon.getMinU, icon.getMaxU, icon.getMinV, icon.getMaxV)
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
    val tes = Tessellator.instance
    val xmin = 0
    val xmax = 1
    val ymin = 0
    val ymax = 1
    val zmin = 0
    val zmax = 1
    tes.addTranslation(x.toFloat, y.toFloat, z.toFloat)
    tes.startDrawingQuads()
    //    tes.setColorRGBA_F(.1f, .1f, .1f, 1.0F)
    tes.addVertex(xmin, ymax, zmin)
    tes.addVertex(xmin, ymax, zmax)
    tes.addVertex(xmax, ymax, zmax)
    tes.addVertex(xmax, ymax, zmin)

    tes.addVertex(xmin, ymin, zmin)
    tes.addVertex(xmax, ymin, zmin)
    tes.addVertex(xmax, ymin, zmax)
    tes.addVertex(xmin, ymin, zmax)

    tes.addVertex(xmin, ymin, zmin)
    tes.addVertex(xmin, ymax, zmin)
    tes.addVertex(xmax, ymax, zmin)
    tes.addVertex(xmax, ymin, zmin)

    tes.addVertex(xmax, ymin, zmin)
    tes.addVertex(xmax, ymax, zmin)
    tes.addVertex(xmax, ymax, zmax)
    tes.addVertex(xmax, ymin, zmax)

    tes.addVertex(xmin, ymin, zmax)
    tes.addVertex(xmax, ymin, zmax)
    tes.addVertex(xmax, ymax, zmax)
    tes.addVertex(xmin, ymax, zmax)

    tes.addVertex(xmin, ymin, zmin)
    tes.addVertex(xmin, ymin, zmax)
    tes.addVertex(xmin, ymax, zmax)
    tes.addVertex(xmin, ymax, zmin)
    tes.addTranslation(-x.toFloat, -y.toFloat, -z.toFloat)
    tes.draw()
  }
}
