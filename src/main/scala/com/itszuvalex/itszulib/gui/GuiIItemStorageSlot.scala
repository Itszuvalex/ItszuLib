package com.itszuvalex.itszulib.gui

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.IItemStack
import com.itszuvalex.itszulib.container.sync.{ISync, SyncItemStorageItemStack}
import com.itszuvalex.itszulib.util.Color
import net.minecraft.client.gui.Gui
import org.apache.logging.log4j.Level
import org.lwjgl.opengl.GL11

/**
  * Created by Chris on 12/17/2016.
  */
class GuiIItemStorageSlot(anchorX: Int, anchorY: Int, storage: IItemStorage, slot: Int, shouldRender: () => Boolean = () => true, str: String = null) extends
  GuiItemStack(anchorX, anchorY, shouldRender, str) {

  var sync: ISync[IItemStack] = _

  def findMatchingSync(syncs: Iterable[ISync[_]]): Unit = {
    sync = syncs.find {
      case storage: SyncItemStorageItemStack if storage.storage == this.storage && storage.storageIndex == slot => true
        ItszuLib.logger.log(Level.WARN, "Found matching sync.")
        true
      case _ => false
    }.orNull.asInstanceOf[ISync[IItemStack]]
  }

  override def itemStack: IItemStack = storage(slot)

  override def render(screenX: Int, screenY: Int, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    super.render(screenX, screenY, mouseX, mouseY, partialTicks)

    if (renderHighlight) {
      GL11.glPushMatrix()
      GL11.glTranslatef(0.0F, 0.0F, 512.0F)
      Gui.drawRect(screenX + 1, screenY + 1, screenX + panelWidth - 1, screenY + panelHeight - 1, Color(150.toByte, 255.toByte, 255.toByte, 255.toByte).toInt)
      GL11.glPopMatrix()
    }
  }

  def renderHighlight: Boolean = isMousedOver
}
