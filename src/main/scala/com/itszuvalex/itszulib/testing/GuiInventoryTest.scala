package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.gui.{GuiBase, GuiProgress}
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

  add(new GuiProgress(0, 0, 20, 5, () => (player.getEntityWorld.getWorldTime % 10 * 10f) / 100f))
  add(new GuiProgress(0, 5, 20, 5, () => (player.getEntityWorld.getWorldTime % 10 * 10f) / 100f, direction = GuiProgress.RightLeft))
  add(new GuiProgress(0, 10, 5, 20, () => (player.getEntityWorld.getWorldTime % 10 * 10f) / 100f, direction = GuiProgress.TopDown))
  add(new GuiProgress(5, 10, 5, 20, () => (player.getEntityWorld.getWorldTime % 10 * 10f) / 100f, direction = GuiProgress.BottomUp))

  override def GuiID: Int = 1

  override def drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int): Unit = {
    GL11.glColor4f(1, 1, 1, 1)
    Minecraft.getMinecraft.getTextureManager.bindTexture(GuiInventoryTest.texture)
    val k = (width - xSize) / 2
    val l = (height - ySize) / 2
    drawTexturedModalRect(k, l, 0, 0, xSize, ySize)

    super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY)
  }

}
