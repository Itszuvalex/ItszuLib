package com.itszuvalex.itszulib.gui

import com.itszuvalex.itszulib.api.storage.ItemStoragePlayerInventory
import com.itszuvalex.itszulib.container.ContainerBase
import com.itszuvalex.itszulib.container.sync.SyncItemStorageItemStack
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.InventoryPlayer

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
  * Created by Christopher Harris (Itszuvalex) on 10/19/14.
  */
abstract class GuiBase(c: ContainerBase) extends GuiContainer(c) with GuiPanel {

  override def _panelWidth = xSize

  override def _panelWidth_=(_width: Int) = {xSize = _width}

  override def _panelHeight = ySize

  override def _panelHeight_=(_height: Int) = {ySize = _height}

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

  override def anchorX_=(_x: Int) = {guiLeft = _x}

  override def anchorY = guiTop

  override def anchorY_=(_y: Int) = {guiTop = _y}

  override def updateScreen(): Unit = {
    super.updateScreen()
    subElements.foreach(_.update())
  }


  override def drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int): Unit = {
    renderUpdate(anchorX, anchorY, mouseX - anchorX, mouseY - anchorY, partialTicks)
    val tooltipList = new ListBuffer[String]
    subElements.foreach(gui => if (gui.isMousedOver) gui.addTooltip(mouseX, mouseY, tooltipList))
    if (tooltipList.nonEmpty) drawHoveringText(tooltipList.toList, mouseX, mouseY, fontRendererObj)
  }

  protected def addPlayerInventorySlots(inventoryPlayer: InventoryPlayer) {
    addPlayerInventorySlots(inventoryPlayer, 7, 83)
  }

  protected def addPlayerInventorySlots(inventoryPlayer: InventoryPlayer, inventoryXStart: Int, inventoryYStart: Int): Unit = {
    val storage = new ItemStoragePlayerInventory(inventoryPlayer)

    (0 until 9).foreach { i =>
      val gui = new GuiIItemStorageSlot(inventoryXStart + i * 18, inventoryYStart + 58, storage, i)
      gui.sync = new SyncItemStorageItemStack(storage, i)
      this.add(gui)
      c.addSync(gui.sync)
    }
    (0 until 3).foreach { i =>
      (0 until 9).foreach { j =>
        val gui = new GuiIItemStorageSlot(inventoryXStart + j * 18, inventoryYStart + i * 18, storage, j + i * 9 + 9)
        gui.sync = new SyncItemStorageItemStack(storage, j + i * 9 + 9)
        this.add(gui)
        c.addSync(gui.sync)
      }
    }
  }
}
