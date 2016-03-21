package com.itszuvalex.itszulib.gui

import com.itszuvalex.itszulib.util.Color
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.Gui
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
  * Created by Christopher Harris (Itszuvalex) on 9/3/15.
  */
object GuiButton {
  val DEFAULT_RAISED_COLOR    = Color(255.toByte, 64, 64, 64).toInt
  val DEFAULT_LOWERED_COLOR   = Color(255.toByte, 15, 15, 15).toInt
  val DEFAULT_BUTTON_COLOR    = Color(255.toByte, 40, 40, 40).toInt
  val DEFAULT_HIGHLIGHT_COLOR = Color(60, 45, 0, 110).toInt
  val DEFAULT_FONT_COLOR      = Color(255.toByte, 255.toByte, 255.toByte, 255.toByte).toInt
}

class GuiButton(override var anchorX: Int,
                override var anchorY: Int,
                override var _panelWidth: Int,
                override var _panelHeight: Int,
                var text: String = "") extends GuiPanel {
  var colorRaised    = GuiButton.DEFAULT_RAISED_COLOR
  var colorLowered   = GuiButton.DEFAULT_LOWERED_COLOR
  var colorDefault   = GuiButton.DEFAULT_BUTTON_COLOR
  var colorHighlight = GuiButton.DEFAULT_HIGHLIGHT_COLOR
  var colorFont      = GuiButton.DEFAULT_FONT_COLOR

  var disabled = false

  override def onMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean = {
    if (!isDisabled && isLocationInside(mouseX, mouseY)) {
      Minecraft.getMinecraft.getSoundHandler.playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F))
      true
    }
    else false
  }

  override def addTooltip(mouseX: Int, mouseY: Int, tooltip: ListBuffer[String]): Unit = {
  }

  override def update(): Unit = {}

  override def render(screenX: Int, screenY: Int, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    super.render(screenX, screenY, mouseX, mouseY, partialTicks)
    GL11.glColor3f(1, 1, 1)
    //Top raised rect
    Gui.drawRect(screenX, screenY, screenX + panelWidth, screenY + 1, if (isDisabled) colorLowered else colorRaised)
    //Left raised rect
    Gui.drawRect(screenX, screenY + 1, screenX + 1, screenY + panelHeight - 1, if (isDisabled) colorLowered else colorRaised)
    //Bottom lowered rect
    Gui.drawRect(screenX, screenY + panelHeight - 1, screenX + panelWidth, screenY + panelHeight, if (isDisabled) colorRaised else colorLowered)
    //Right lowered rect
    Gui.drawRect(screenX + panelWidth - 1, screenY + 1, screenX + panelWidth, screenY + panelHeight - 1, if (isDisabled) colorRaised else colorLowered)
    //Main rect
    Gui.drawRect(screenX + 1, screenY + 1, screenX + panelWidth - 1, screenY + panelHeight - 1, colorDefault)

    if (!isDisabled && isMousedOver)
      Gui.drawRect(screenX, screenY, screenX + panelWidth, screenY + panelHeight, colorHighlight)

    val fr = Minecraft.getMinecraft.fontRenderer
    val lines = fr.listFormattedStringToWidth(text, panelWidth - 2).asInstanceOf[java.util.List[String]]
    var height = 0
    lines.foreach { _ =>
      if (height == 0 || !((height + fr.FONT_HEIGHT) < (panelHeight - 2))) height += fr.FONT_HEIGHT
                  }
    val yBuff = (panelHeight - height - 2) / 2
    var yOffset = 0
    lines.foreach { line =>
      val xBuff = (panelWidth - fr.getStringWidth(line) - 2) / 2
      fr.drawString(line, screenX + xBuff + 1, screenY + yBuff + 1 + yOffset, colorFont)
      yOffset += fr.FONT_HEIGHT
                  }
  }

  def isDisabled = disabled
}
