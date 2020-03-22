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

import com.itszuvalex.itszulib.api.wrappers.{Converter, IWorld}
import com.itszuvalex.itszulib.gui.GuiStack
import com.itszuvalex.itszulib.initialization.{BlockBuilder, ItemBuilder}
import com.itszuvalex.itszulib.render.{PreviewableRenderHandler, ShaderUtils}
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.{DimensionManager, MinecraftForge}
import net.minecraftforge.fml.relauncher.Side


class ProxyClient extends ProxyCommon {
  override def registerRendering() {
    super.registerRendering()
    ShaderUtils.init()
    MinecraftForge.EVENT_BUS.register(new PreviewableRenderHandler)

    // Previewable Rendering Test
    GuiStack.init()
  }

  override def getClientGuiElement(ID: Int, data: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef = {
    (ID, world.getTileEntity(new BlockPos(x, y, z))) match {
      case (_, _) => null
    }
  }

  override def side: Side = Side.CLIENT

  override def addScheduledTask(f: () => Unit): Unit = Minecraft.getMinecraft.addScheduledTask(new Runnable {override def run(): Unit = f()})

  override def getIWorld(id: Int): IWorld = Converter.IWorldFromWorld(Option(DimensionManager.getWorld(id)).getOrElse(Minecraft.getMinecraft.world))

  override def getWorld(id: Int): World = Option(DimensionManager.getWorld(id)).getOrElse(Minecraft.getMinecraft.world)


  override def registerItemModel[T <: Item](item: ItemBuilder[T]): Unit = {
    Minecraft.getMinecraft.getRenderItem.getItemModelMesher.register(item.build(), 0, new ModelResourceLocation(item.registryName.get, "inventory"))
  }

  override def registerBlockModel[B <: Block](block: BlockBuilder[B]): Unit = {
    Minecraft.getMinecraft.getRenderItem.getItemModelMesher.register(Item.getItemFromBlock(block.build()), 0, new ModelResourceLocation(block.registryName.get, "inventory"))
  }
}