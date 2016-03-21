package com.itszuvalex.itszulib.gui

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
  * Created by Christopher Harris (Itszuvalex) on 10/19/14.
  */
abstract class GuiBase(c: Container) extends GuiContainer(c) with GuiPanel {
  def isPointInRegion(x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int) = func_146978_c(x, y, width, height, mouseX, mouseY)

  override def _panelWidth = xSize

  override def _panelWidth_=(_width: Int) = { xSize = _width }

  override def _panelHeight = ySize

  override def _panelHeight_=(_height: Int) = { ySize = _height }

  override def mouseClicked(mouseX: Int, mouseY: Int, button: Int): Unit = {
    val atb = GuiTextBox.activeTextBox
    if (atb != null && !atb.isLocationInside(mouseX - atb.anchorX - anchorX, mouseY - atb.anchorY - anchorY)) {
      atb.setFocused(false)
    }
    if (!subElements.exists(gui => gui.onMouseClick(mouseX - gui.anchorX - anchorX,
                                                    mouseY - gui.anchorY - anchorY,
                                                    button)))
      super.mouseClicked(mouseX, mouseY, button)
  }

  override def anchorX = guiLeft

  override def anchorX_=(_x: Int) = { guiLeft = _x }

  override def anchorY = guiTop

  override def anchorY_=(_y: Int) = { guiTop = _y }

  override def updateScreen(): Unit = {
    super.updateScreen()
    subElements.foreach(_.update())
  }

  override def drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    super.drawScreen(mouseX, mouseY, partialTicks)
    renderUpdate(anchorX, anchorY, mouseX - anchorX, mouseY - anchorY, partialTicks)
    val tooltipList = new ListBuffer[String]
    subElements.foreach(gui => if (gui.isMousedOver) gui.addTooltip(mouseX, mouseY, tooltipList))
    if (tooltipList.nonEmpty) drawHoveringText(tooltipList.toList, mouseX, mouseY, fontRendererObj)
  }
}
