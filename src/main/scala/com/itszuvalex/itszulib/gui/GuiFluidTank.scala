package com.itszuvalex.itszulib.gui

import com.itszuvalex.itszulib.core.traits.tile.{TileFluidTank, TileMultiFluidTank}
import com.itszuvalex.itszulib.network.PacketHandler
import com.itszuvalex.itszulib.network.messages.MessageFluidSlotClick
import com.itszuvalex.itszulib.render.RenderUtils
import com.itszuvalex.itszulib.util.Color
import net.minecraft.client.gui.Gui
import net.minecraft.util.{EnumChatFormatting, IIcon}
import net.minecraftforge.fluids._

import scala.collection.mutable.ListBuffer

/**
  * Created by Alex on 04.10.2015.
  */
object GuiFluidTank {
  val DEFAULT_RAISED_COLOR     = Color(255.toByte, 64, 64, 64).toInt
  val DEFAULT_LOWERED_COLOR    = Color(255.toByte, 15, 15, 15).toInt
  val DEFAULT_BACKGROUND_COLOR = Color(255.toByte, 40, 40, 40).toInt
  val DEFAULT_FLUID_BACK_COLOR = Color(255.toByte, 255.toByte, 255.toByte, 255.toByte).toInt
  val DEFAULT_SCALE_COLOR      = Color(255.toByte, 200.toByte, 0, 0).toInt
}

/**
  * Fluid display slot that allows manual player interaction if desired.
  *
  * @param anchorX
  * @param anchorY
  * @param gui            GuiBase object
  * @param tileSingleTank TileFluidTank TE, only required if tileMultiTank is null.
  * @param tileMultiTank  TileMultiFluidTank TE, only required if tileSingleTank is null.
  * @param tankID         ID of the tank in tileMultiTank, -1 if tileSingleTank is used instead.
  * @param manualAccess   0 = no access, 1 = input only, 2 = output only, 3 = both
  * @param filterFluid    Only this fluid will be accepted, null means any fluid
  * @param drawTank       If false, only draws the fluid itself without tank and scale
  */
class GuiFluidTank(override var anchorX: Int,
                   override var anchorY: Int,
                   var gui: GuiBase,
                   var tileSingleTank: TileFluidTank,
                   var tileMultiTank: TileMultiFluidTank,
                   var tankID: Int,
                   var manualAccess: Int,
                   var filterFluid: Fluid,
                   var drawTank: Boolean) extends GuiPanel {

  override var _panelWidth : Int = 18
  override var _panelHeight: Int = 66
  var colorRaised     = GuiFluidTank.DEFAULT_RAISED_COLOR
  var colorLowered    = GuiFluidTank.DEFAULT_LOWERED_COLOR
  var colorBackground = GuiFluidTank.DEFAULT_BACKGROUND_COLOR
  var colorFluidBack  = GuiFluidTank.DEFAULT_FLUID_BACK_COLOR
  var colorScale      = GuiFluidTank.DEFAULT_SCALE_COLOR

  /**
    * Constructor that only uses a TileFluidTank. See main constructor for param description.
    */
  def this(x: Int, y: Int, guiObj: GuiBase, tile: TileFluidTank, manAccess: Int, filtFluid: Fluid, _drawTank: Boolean) =
    this(x, y, guiObj, tile, null, -1, manAccess, filtFluid, _drawTank)

  /**
    * Constructor that only uses a TileMultiFluidTank and Tank ID. See main constructor for param description.
    */
  def this(x: Int, y: Int, guiObj: GuiBase, tile: TileMultiFluidTank, _tankID: Int, manAccess: Int, filtFluid: Fluid, _drawTank: Boolean) =
    this(x, y, guiObj, null, tile, _tankID, manAccess, filtFluid, _drawTank)

  override def addTooltip(mouseX: Int, mouseY: Int, tooltip: ListBuffer[String]): Unit = {
    super.addTooltip(mouseX, mouseY, tooltip)
    tooltip ++= getTooltip
  }

  def getTooltip: List[String] = {
    var ret = List.empty[String]
    tankID match {
      case -1 =>
        ret :+= ("Fluid: " + (if (tileSingleTank.tank.getFluid == null) "None" else tileSingleTank.tank.getFluid.getFluid.getLocalizedName(tileSingleTank.tank.getFluid) + ", " + tileSingleTank.tank.getFluidAmount + "mB"))
        ret :+= ("Capacity: " + tileSingleTank.tank.getCapacity + "mB")
      case _ =>
        ret :+= ("Fluid: " + (if (tileMultiTank.tanks(tankID).getFluid == null) "None" else tileMultiTank.tanks(tankID).getFluid.getFluid.getLocalizedName(tileMultiTank.tanks(tankID).getFluid) + ", " + tileMultiTank.tanks(tankID).getFluidAmount + "mB"))
        ret :+= ("Capacity: " + tileMultiTank.tanks(tankID).getCapacity + "mB")
    }
    if ((manualAccess & 1) == 1) {
      if (filterFluid != null) ret :+= ("Accepts: " + filterFluid.getLocalizedName(new FluidStack(filterFluid, 0)))
      ret :+= (EnumChatFormatting.ITALIC + EnumChatFormatting.AQUA.toString + "Left click with fluid" + EnumChatFormatting.RESET)
      ret :+= (EnumChatFormatting.ITALIC + EnumChatFormatting.AQUA.toString + " container to fill tank" + EnumChatFormatting.RESET)
    }
    if ((manualAccess & 2) == 2) {
      ret :+= (EnumChatFormatting.ITALIC + EnumChatFormatting.LIGHT_PURPLE.toString + "Right click with fluid" + EnumChatFormatting.RESET)
      ret :+= (EnumChatFormatting.ITALIC + EnumChatFormatting.LIGHT_PURPLE.toString + " container to drain tank" + EnumChatFormatting.RESET)
    }
    ret
  }

  override def onMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean = {
    if (manualAccess != 0 && isMousedOver) {
      tankID match {
        case -1 =>
          PacketHandler.INSTANCE.sendToServer(new MessageFluidSlotClick(tileSingleTank.xCoord, tileSingleTank.yCoord, tileSingleTank.zCoord, -1, button, manualAccess, if (filterFluid == null) -1 else filterFluid.getID))
        case _ =>
          PacketHandler.INSTANCE.sendToServer(new MessageFluidSlotClick(tileMultiTank.xCoord, tileMultiTank.yCoord, tileMultiTank.zCoord, tankID, button, manualAccess, if (filterFluid == null) -1 else filterFluid.getID))
      }
    }
    super.onMouseClick(mouseX, mouseY, button)
  }

  override def render(screenX: Int, screenY: Int, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    super.render(screenX, screenY, mouseX, mouseY, partialTicks)

    if (drawTank) {
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
      drawFluid(screenX, screenY)
      drawScale(screenX, screenY)
    }
  }

  def drawScale(screenX: Int, screenY: Int): Unit = {
    //Full
    Gui.drawRect(screenX + 1, screenY + 1, screenX + 13, screenY + 2, colorScale)
    //Half
    Gui.drawRect(screenX + 1, screenY + 33, screenX + 13, screenY + 34, colorScale)
    //1 Quarter
    Gui.drawRect(screenX + 1, screenY + 49, screenX + 9, screenY + 50, colorScale)
    //3 Quarters
    Gui.drawRect(screenX + 1, screenY + 17, screenX + 9, screenY + 18, colorScale)
    //1 Eighth
    Gui.drawRect(screenX + 1, screenY + 57, screenX + 5, screenY + 58, colorScale)
    //3 Eighths
    Gui.drawRect(screenX + 1, screenY + 41, screenX + 5, screenY + 42, colorScale)
    //5 Eighths
    Gui.drawRect(screenX + 1, screenY + 25, screenX + 5, screenY + 26, colorScale)
    //7 Eighths
    Gui.drawRect(screenX + 1, screenY + 9, screenX + 5, screenY + 10, colorScale)
  }

  def drawFluid(screenX: Int, screenY: Int): Unit = {
    var height: Int = 0
    var icon: IIcon = null
    tankID match {
      case -1 =>
        if (tileSingleTank.tank.getFluid == null) return
        if (tileSingleTank.tank.getFluid.amount == 0) return
        icon = tileSingleTank.tank.getFluid.getFluid.getStillIcon
        height = math.floor((tileSingleTank.tank.getFluid.amount / tileSingleTank.tank.getCapacity.toDouble) * 64).toInt
      case _ =>
        if (tileMultiTank.tanks(tankID).getFluid == null) return
        if (tileMultiTank.tanks(tankID).getFluid.amount == 0) return
        icon = tileMultiTank.tanks(tankID).getFluid.getFluid.getStillIcon
        height = math.floor((tileMultiTank.tanks(tankID).getFluid.amount / tileMultiTank.tanks(tankID).getCapacity.toDouble) * 64).toInt
    }
    val topPx = screenY + 65 - height

    Gui.drawRect(screenX + 1, topPx, screenX + 17, screenY + 65, colorFluidBack)

    RenderUtils.renderLiquidInGUI(gui, 0, icon, screenX + 1, topPx, 16, height)
  }

}

