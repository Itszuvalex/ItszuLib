package com.itszuvalex.itszulib.gui

import com.itszuvalex.itszulib.util.Color
import net.minecraft.client.Minecraft

/**
  * Created by Christopher Harris (Itszuvalex) on 9/17/15.
  */
object GuiLabel {
  val DEFAULT_FONT_COLOR = Color(255.toByte, 255.toByte, 255.toByte, 255.toByte).toInt
}

class GuiLabel(override var anchorX: Int,
               override var anchorY: Int,
               override var _panelWidth: Int,
               override var _panelHeight: Int,
               var text: String) extends GuiPanel {
  var colorFont = GuiLabel.DEFAULT_FONT_COLOR
  var xPadding  = 0
  var yPadding  = 0

  override def render(screenX: Int, screenY: Int, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    super.render(screenX, screenY, mouseX, mouseY, partialTicks)
    Minecraft.getMinecraft.fontRenderer.drawSplitString(text, screenX + xPadding, screenY + yPadding, panelWidth - 2 * xPadding, colorFont)
  }
}
