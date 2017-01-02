/*
 * ******************************************************************************
 *  * Copyright (C) 2013  Christopher Harris (Itszuvalex)
 *  * Itszuvalex@gmail.com
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * as published by the Free Software Foundation; either version 2
 *  * of the License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *  *****************************************************************************
 */
package com.itszuvalex.itszulib.proxy

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.gui.GuiStack
import com.itszuvalex.itszulib.render.{PreviewableRenderHandler, PreviewableRendererRegistry, ShaderUtils}
import com.itszuvalex.itszulib.testing.{PortalTileTest, _}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.client.ForgeHooksClient
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.relauncher.Side


class ProxyClient extends ProxyCommon {
  override def registerRendering() {
    super.registerRendering()
    ShaderUtils.init()
    MinecraftForge.EVENT_BUS.register(new PreviewableRenderHandler)

    // Previewable Rendering Test
    PreviewableIDs.testID = PreviewableRendererRegistry.bindRenderer(new TestPreviewableRenderer)

    ClientRegistry.bindTileEntitySpecialRenderer[PortalTileTest](classOf[PortalTileTest], new RenderPortalTest)
    ClientRegistry.bindTileEntitySpecialRenderer[TileTankTest](classOf[TileTankTest], new RenderSidedCubeTest)

    ForgeHooksClient.registerTESRItemStack(Item.getItemFromBlock(ItszuLib.blockTankTest), 0, classOf[TileTankTest])
    ForgeHooksClient.registerTESRItemStack(Item.getItemFromBlock(ItszuLib.blockPortalTest), 0, classOf[PortalTileTest])
    Minecraft.getMinecraft.getRenderItem.getItemModelMesher.register(Item.getItemFromBlock(ItszuLib.blockTankTest), 0, new ModelResourceLocation(ItszuLib.ID.toLowerCase() + ":" + "BlockTankTest", "inventory"))
    Minecraft.getMinecraft.getRenderItem.getItemModelMesher.register(Item.getItemFromBlock(ItszuLib.blockPortalTest), 0, new ModelResourceLocation(ItszuLib.ID.toLowerCase() + ":" + "BlockPortalTest", "inventory"))

    GuiStack.init()
  }

  override def getClientGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef = {
    (ID, world.getTileEntity(new BlockPos(x, y, z))) match {
      case (0, te: TileTankTest) => new GuiTankTest(player, player.inventory, te)
      case (1, te: TileInventoryTest) => new GuiInventoryTest(player, player.inventory, te)
      case (_, _) => null
    }
  }

  override def side: Side = Side.CLIENT
}