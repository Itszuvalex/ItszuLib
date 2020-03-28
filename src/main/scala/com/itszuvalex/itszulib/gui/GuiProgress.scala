package com.itszuvalex.itszulib.gui

import com.itszuvalex.itszulib.gui.GuiProgress._
import com.itszuvalex.itszulib.util.Color
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager

import scala.collection.mutable.ListBuffer

/**
  * Created by Chris on 1/15/2017.
  */
object GuiProgress {
  val DEFAULT_RAISED_COLOR    : Int = Color(255.toByte, 64, 64, 64).toInt
  val DEFAULT_LOWERED_COLOR   : Int = Color(255.toByte, 15, 15, 15).toInt
  val DEFAULT_BACKGROUND_COLOR: Int = Color(255.toByte, 40, 40, 40).toInt
  val DEFAULT_FONT_COLOR      : Int = Color(255.toByte, 255.toByte, 255.toByte, 255.toByte).toInt
  val DEFAULT_PROGRESS_COLOR  : Int = Color(255.toByte, 255.toByte, 255.toByte, 255.toByte).toInt


  trait RenderDirection

  object TopDown extends RenderDirection

  object BottomUp extends RenderDirection

  object LeftRight extends RenderDirection

  object RightLeft extends RenderDirection

}

class GuiProgress(override var anchorX: Int,
                  override var anchorY: Int,
                  override var _panelWidth: Int,
                  override var _panelHeight: Int,
                  var progress: () => Float,
                  drawOutline: => Boolean = true,
                  drawBackground: => Boolean = true,
                  var direction: RenderDirection = LeftRight) extends GuiPanel {

  var colorRaised    : Int = GuiProgress.DEFAULT_RAISED_COLOR
  var colorLowered   : Int = GuiProgress.DEFAULT_LOWERED_COLOR
  var colorBackground: Int = GuiProgress.DEFAULT_BACKGROUND_COLOR
  var colorFont      : Int = GuiProgress.DEFAULT_FONT_COLOR
  var colorProgress  : Int = GuiProgress.DEFAULT_PROGRESS_COLOR

  override def addTooltip(mouseX: Int, mouseY: Int, tooltip: ListBuffer[String]): Unit = {
    super.addTooltip(mouseX, mouseY, tooltip)
  }

  override def render(screenX: Int, screenY: Int, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    super.render(screenX, screenY, mouseX, mouseY, partialTicks)

    GlStateManager.disableLighting()
    if (drawBackground) {
      //Main rect
      Gui.drawRect(screenX, screenY, screenX + panelWidth, screenY + panelHeight, colorBackground)
    }

    direction match {
      case TopDown => Gui.drawRect(screenX, screenY, screenX + panelWidth, screenY + (panelHeight * progress()).toInt, colorProgress)
      case BottomUp => Gui.drawRect(screenX, screenY + (panelHeight * (1f - progress())).toInt, screenX + panelWidth, screenY + panelHeight, colorProgress)
      case LeftRight => Gui.drawRect(screenX, screenY, screenX + (panelWidth * progress()).toInt, screenY + panelHeight, colorProgress)
      case RightLeft => Gui.drawRect(screenX + (panelWidth * (1f - progress())).toInt, screenY, screenX + panelWidth, screenY + panelHeight, colorProgress)
      case _ =>
    }

    if (drawOutline) {
      //Top lowered rect
      Gui.drawRect(screenX, screenY, screenX + panelWidth, screenY + 1, colorLowered)
      //Left lowered rect
      Gui.drawRect(screenX, screenY + 1, screenX + 1, screenY + panelHeight - 1, colorLowered)
      //Bottom raised rect
      Gui.drawRect(screenX, screenY + panelHeight - 1, screenX + panelWidth, screenY + panelHeight, colorRaised)
      //Right raised rect
      Gui.drawRect(screenX + panelWidth - 1, screenY + 1, screenX + panelWidth, screenY + panelHeight - 1, colorRaised)
    }
  }
}

