package com.itszuvalex.itszulib.gui

import com.itszuvalex.itszulib.render.RenderUtils._
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

/**
  * Created by Christopher Harris (Itszuvalex) on 1/26/17.
  */
class GuiPanelTexture(override var anchorX: Int,
                      override var anchorY: Int,
                      override var _panelWidth: Int,
                      override var _panelHeight: Int,
                      var tex: ResourceLocation)
  extends GuiPanel {
  override def render(screenX: Int, screenY: Int, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    super.render(screenX, screenY, mouseX, mouseY, partialTicks)
    GL11.glColor3f(1, 1, 1)
    GL11.glEnable(GL11.GL_BLEND)
    Minecraft.getMinecraft.getTextureManager.bindTexture(tex)
    drawBlock(DefaultVertexFormats.POSITION_TEX) {
      addVertexUV(screenX, screenY + panelHeight, 0, 0, 1f)
      addVertexUV(screenX + panelWidth, screenY + panelHeight, 0, 1f, 1f)
      addVertexUV(screenX + panelWidth, screenY, 0, 1f, 0)
      addVertexUV(screenX, screenY, 0, 0, 0)
    }
    GL11.glDisable(GL11.GL_BLEND)
  }
}
