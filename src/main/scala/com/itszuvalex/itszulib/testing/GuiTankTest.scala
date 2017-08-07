package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.gui.{GuiBase, GuiFluidTank, GuiLabel}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.util.{EnumFacing, ResourceLocation}
import org.lwjgl.opengl.GL11

/**
  * Created by Alex on 12.10.2015.
  */
object GuiTankTest {
  val texture = new ResourceLocation("itszulib", "textures/gui/guiinventorybase.png")
}

class GuiTankTest(player: EntityPlayer, inv: InventoryPlayer, private val tile: TileTankTest, val data: Int) extends GuiBase(new ContainerTankTest(player, inv, tile, false)) {

  val tank1 = new GuiFluidTank(19, 9, this, tile, 0, 3, null, true)
  val tank2 = new GuiFluidTank(49, 9, this, tile, 1, 3, null, true)
  val tank3 = new GuiFluidTank(79, 9, this, tile, 2, 3, null, true)

  val sideText = new GuiLabel(109, 9, 40, 10, EnumFacing.values()(data).getName)

  tank1.setShouldRender(false)
  tank2.setShouldRender(false)
  tank3.setShouldRender(false)
  sideText.setShouldRender(false)

  add(tank1, tank2, tank3, sideText)

  addPlayerInventorySlots(inv)

  override def GuiID: Int = 0


  override def drawGuiContainerBackgroundLayer(partialTicks: Float, x: Int, y: Int): Unit = {
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
    Minecraft.getMinecraft.getTextureManager.bindTexture(GuiTankTest.texture)
    val k = (width - xSize) / 2
    val l = (height - ySize) / 2
    drawTexturedModalRect(k, l, 0, 0, xSize, ySize)


    tank1.render(anchorX + tank1.anchorX, anchorY + tank1.anchorY, x - anchorX - tank1.anchorX, y - anchorY - tank1.anchorY, partialTicks)
    tank2.render(anchorX + tank2.anchorX, anchorY + tank2.anchorY, x - anchorX - tank2.anchorX, y - anchorY - tank2.anchorY, partialTicks)
    tank3.render(anchorX + tank3.anchorX, anchorY + tank3.anchorY, x - anchorX - tank3.anchorX, y - anchorY - tank3.anchorY, partialTicks)
    sideText.render(anchorX + sideText.anchorX, anchorY + sideText.anchorY, x - anchorX - sideText.anchorX, y - anchorY - sideText.anchorY, partialTicks)

    super.drawGuiContainerBackgroundLayer(partialTicks, x, y)
  }
}
