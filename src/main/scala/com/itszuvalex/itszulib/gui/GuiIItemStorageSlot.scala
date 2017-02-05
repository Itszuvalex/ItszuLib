package com.itszuvalex.itszulib.gui

import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.{Converter, IItemStack}
import com.itszuvalex.itszulib.container.sync.ISync
import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import com.itszuvalex.itszulib.network.messages.MessageIItemStackSyncClick
import com.itszuvalex.itszulib.util.Color
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

/**
  * Created by Chris on 12/17/2016.
  */
class GuiIItemStorageSlot(anchorX: Int, anchorY: Int, storage: IItemStorage, slot: Int, shouldRender: () => Boolean = () => true, str: String = null) extends
  GuiItemStack(anchorX, anchorY, shouldRender, str) {

  var sync: ISync[IItemStack] = _

  override def itemStack: IItemStack = storage(slot)

  override def render(screenX: Int, screenY: Int, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    super.render(screenX, screenY, mouseX, mouseY, partialTicks)

    renderOutline(screenX, screenY, mouseX, mouseY, partialTicks)

    if (renderHighlight) {
      GL11.glPushMatrix()
      GL11.glTranslatef(0.0F, 0.0F, 512.0F)
      Gui.drawRect(screenX + 1, screenY + 1, screenX + panelWidth - 1, screenY + panelHeight - 1, Color(150.toByte, 255.toByte, 255.toByte, 255.toByte).toInt)
      GL11.glPopMatrix()
    }
  }

  def renderHighlight: Boolean = isMousedOver

  def renderOutline(screenX: Int, screenY: Int, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    val item = Converter.IItemStackFromItemStack(Minecraft.getMinecraft.player.inventory.getItemStack)
    if (item.isEmpty) return

    var color: Int = 0
    val transparency = 50.toByte
    val slotEmpty = itemStack.isEmpty

    if (storage.canInsert(slot, item)) {
      if (slotEmpty) {
        color = Color(transparency, 0.toByte, 255.toByte, 0.toByte).toInt
      } else if (item.isItemEqual(itemStack)) {
        color = Color(transparency, 0.toByte, 255.toByte, 255.toByte).toInt
      } else {
        color = Color(transparency, 255.toByte, 0.toByte, 255.toByte).toInt
      }
    } else {
      if (!slotEmpty && item.isItemEqual(itemStack)) {
        color = Color(transparency, 0.toByte, 0.toByte, 255.toByte).toInt
      } else {
        color = Color(transparency, 255.toByte, 0.toByte, 0.toByte).toInt
      }
    }

    Gui.drawRect(screenX, screenY, screenX + 18, screenY + 1, color) // Top
    Gui.drawRect(screenX, screenY + 1, screenX + 1, screenY + 17, color) // Left
    Gui.drawRect(screenX + 17, screenY + 1, screenX + 18, screenY + 17, color) // Right
    Gui.drawRect(screenX, screenY + 17, screenX + 18, screenY + 18, color) // Bot
  }

  def isShiftHeld: Boolean = Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54)

  override def onMouseClick(mouseX: Int, mouseY: Int, button: Int): Boolean = {
    if (isLocationInside(mouseX, mouseY)) {
      val click = button match {
        case 0 => if (isShiftHeld) MessageIItemStackSyncClick.SHIFT_LEFT_CLICK else MessageIItemStackSyncClick.LEFT_CLICK
        case 1 => MessageIItemStackSyncClick.RIGHT_CLICK
        case 2 => MessageIItemStackSyncClick.MIDDLE_CLICK
        case _ => MessageIItemStackSyncClick.UNKNOWN
      }
      ItszuLibPacketHandler.INSTANCE.sendToServer(new MessageIItemStackSyncClick(sync, click))
      true
    } else false
  }

  override def onMouseRelease(mouseX: Int, mouseY: Int, button: Int): Boolean = {
    if (isLocationInside(mouseX, mouseY)) {
      val click = button match {
        case 0 => MessageIItemStackSyncClick.LEFT_RELEASE
        case 1 => MessageIItemStackSyncClick.RIGHT_RELEASE
        case _ => MessageIItemStackSyncClick.UNKNOWN
      }
      ItszuLibPacketHandler.INSTANCE.sendToServer(new MessageIItemStackSyncClick(sync, click))
      true
    } else false
  }

  override def onMouseClickMove(mouseX: Int, mouseY: Int, button: Int, timeSinceLastClick: Long): Boolean = {
    if (isLocationInside(mouseX, mouseY)) {
      val click = button match {
        case 0 => MessageIItemStackSyncClick.LEFT_DRAG
        case 1 => MessageIItemStackSyncClick.RIGHT_DRAG
        case _ => MessageIItemStackSyncClick.UNKNOWN
      }
      ItszuLibPacketHandler.INSTANCE.sendToServer(new MessageIItemStackSyncClick(sync, click))
      true
    } else false
  }

}
