package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.gui.{GuiBase, GuiFluidTank}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

/**
  * Created by Alex on 12.10.2015.
  */
object GuiTankTest {
  val texture = new ResourceLocation("itszulib", "textures/gui/GuiInventoryBase.png")
}

class GuiTankTest(player: EntityPlayer, inv: InventoryPlayer, private val tile: TileTankTest) extends GuiBase(new ContainerTankTest(player, inv, tile)) {

  val tank1 = new GuiFluidTank(19, 9, this, tile, 0, 3, null, true)
  val tank2 = new GuiFluidTank(49, 9, this, tile, 1, 3, null, true)
  val tank3 = new GuiFluidTank(79, 9, this, tile, 2, 3, null, true)

  tank1.setShouldRender(false)
  tank2.setShouldRender(false)
  tank3.setShouldRender(false)

  add(tank1, tank2, tank3)

  override def drawGuiContainerBackgroundLayer(p_146976_1_ : Float, p_146976_2_ : Int, p_146976_3_ : Int): Unit = {
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
    Minecraft.getMinecraft.getTextureManager.bindTexture(GuiTankTest.texture)
    val k = (width - xSize) / 2
    val l = (height - ySize) / 2
    drawTexturedModalRect(k, l, 0, 0, xSize, ySize)


    tank1.render(anchorX + tank1.anchorX, anchorY + tank1.anchorY, p_146976_2_ - anchorX - tank1.anchorX, p_146976_3_ - anchorY - tank1.anchorY, p_146976_1_)
    tank2.render(anchorX + tank2.anchorX, anchorY + tank2.anchorY, p_146976_2_ - anchorX - tank2.anchorX, p_146976_3_ - anchorY - tank2.anchorY, p_146976_1_)
    tank3.render(anchorX + tank3.anchorX, anchorY + tank3.anchorY, p_146976_2_ - anchorX - tank3.anchorX, p_146976_3_ - anchorY - tank3.anchorY, p_146976_1_)
  }
}
