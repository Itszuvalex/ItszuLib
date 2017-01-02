package com.itszuvalex.itszulib.gui

import com.itszuvalex.itszulib.api.wrappers.IItemStack
import com.itszuvalex.itszulib.util.Color
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.{FontRenderer, Gui}
import net.minecraft.client.renderer.{GlStateManager, RenderHelper, RenderItem}
import org.lwjgl.opengl.GL11

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
  * Created by Christopher Harris (Itszuvalex) on 9/4/15.
  */
object GuiItemStack {
  val DEFAULT_RAISED_COLOR     = Color(255.toByte, 64, 64, 64).toInt
  val DEFAULT_LOWERED_COLOR    = Color(255.toByte, 15, 15, 15).toInt
  val DEFAULT_BACKGROUND_COLOR = Color(255.toByte, 40, 40, 40).toInt
  val DEFAULT_FONT_COLOR       = Color(255.toByte, 255.toByte, 255.toByte, 255.toByte).toInt
}

abstract class GuiItemStack(override var anchorX: Int,
  override var anchorY: Int,
  var drawSlot: () => Boolean = () => true,
  var str: String = null) extends GuiPanel {

  val itemRenderer: RenderItem   = Minecraft.getMinecraft.getRenderItem
  val fontRenderer: FontRenderer = Minecraft.getMinecraft.fontRendererObj
  override var _panelHeight: Int = 18
  override var _panelWidth : Int = 18
  var colorRaised     = GuiItemStack.DEFAULT_RAISED_COLOR
  var colorLowered    = GuiItemStack.DEFAULT_LOWERED_COLOR
  var colorBackground = GuiItemStack.DEFAULT_BACKGROUND_COLOR
  var colorFont       = GuiItemStack.DEFAULT_FONT_COLOR

  def itemStack: IItemStack

  override def addTooltip(mouseX: Int, mouseY: Int, tooltip: ListBuffer[String]): Unit = {
    super.addTooltip(mouseX, mouseY, tooltip)
    itemStack match {
      case null =>
      case a if a.isEmpty =>
      case i => tooltip ++= i.toMinecraft.getTooltip(Minecraft.getMinecraft.thePlayer, Minecraft.getMinecraft.gameSettings.advancedItemTooltips)
    }
  }

  override def render(screenX: Int, screenY: Int, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    super.render(screenX, screenY, mouseX, mouseY, partialTicks)

    if (drawSlot()) {
      GlStateManager.disableLighting()
      //Top lowered rect
      Gui.drawRect(screenX, screenY, screenX + panelWidth, screenY + 1, colorLowered)
      //Left lowered rect
      Gui.drawRect(screenX, screenY + 1, screenX + 1, screenY + panelHeight - 1, colorLowered)
      //Bottom raised rect
      Gui.drawRect(screenX, screenY + panelHeight - 1, screenX + panelWidth, screenY + panelHeight, colorRaised)
      //Right raised rect
      Gui.drawRect(screenX + panelWidth - 1, screenY + 1, screenX + panelWidth, screenY + panelHeight - 1, colorRaised)
      //Main rect
      Gui.drawRect(screenX + 1, screenY + 1, screenX + panelWidth - 1, screenY + panelHeight - 1, colorBackground)
    }

    drawItemStack(screenX + 1, screenY + 1, str)
  }

  def drawItemStack(locX: Int, locY: Int, amt: String) {
    if (itemStack == null || itemStack.isEmpty) return

    GlStateManager.pushMatrix()
    GlStateManager.translate(0, 0, 32)
    //      this.zLevel = 200.0F
    GlStateManager.color(1, 1, 1)
    itemRenderer.zLevel = 200.0F
    var font: FontRenderer = null
    font = itemStack.item.getFontRenderer(itemStack.toMinecraft)
    if (font == null) font = fontRenderer
    RenderHelper.enableGUIStandardItemLighting()
    itemRenderer.renderItemAndEffectIntoGUI(itemStack.toMinecraft, locX, locY)
    itemRenderer.renderItemOverlayIntoGUI(font, itemStack.toMinecraft, locX, locY, amt)
    itemRenderer.zLevel = 0.0F
    GlStateManager.popMatrix()
  }
}
