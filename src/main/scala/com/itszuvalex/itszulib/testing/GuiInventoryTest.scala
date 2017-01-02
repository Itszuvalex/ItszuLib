package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.container.ContainerBase
import com.itszuvalex.itszulib.container.sync.SyncItemStorageItemStack
import com.itszuvalex.itszulib.gui.{GuiBase, GuiIItemStorageSlot}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

/**
  * Created by Alex on 02.01.2017.
  */
object GuiInventoryTest {
  val texture = new ResourceLocation("itszulib", "textures/gui/guiinventorybase.png")
}

class GuiInventoryTest(player: EntityPlayer, inv: InventoryPlayer, private val tile: TileInventoryTest) extends GuiBase(new ContainerInventoryTest(player, inv, tile, false)) {

  addGuiAndSync(tile.storage, 0, 50, 33)
  addGuiAndSync(tile.storage, 1, 79, 33)
  addGuiAndSync(tile.storage, 2, 108, 33)

  addPlayerInventorySlots(inv)

  override def drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int): Unit = {
    GL11.glColor4f(1, 1, 1, 1)
    Minecraft.getMinecraft.getTextureManager.bindTexture(GuiInventoryTest.texture)
    val k = (width - xSize) / 2
    val l = (height - ySize) / 2
    drawTexturedModalRect(k, l, 0, 0, xSize, ySize)

    super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY)
  }

}
