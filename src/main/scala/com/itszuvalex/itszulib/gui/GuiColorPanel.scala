package com.itszuvalex.itszulib.gui

import net.minecraft.client.gui.Gui

/**
  * Created by Christopher Harris (Itszuvalex) on 9/3/15.
  */
class GuiColorPanel(override var anchorX: Int,
                    override var anchorY: Int,
                    override var _panelWidth: Int,
                    override var _panelHeight: Int,
                    var color: Int) extends GuiPanel {

  override def render(screenX: Int, screenY: Int, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    Gui.drawRect(screenX, screenY, screenX + panelWidth, screenY + panelHeight, color)
  }
}

