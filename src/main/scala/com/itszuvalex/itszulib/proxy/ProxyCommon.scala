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

import com.itszuvalex.itszulib.api.wrappers.IWorld
import com.itszuvalex.itszulib.gui.ItszuGuiHandler
import com.itszuvalex.itszulib.initialization.{BlockBuilder, ItemBuilder}
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side

abstract class ProxyCommon extends ItszuGuiHandler {
  def preInit(): Unit = {}

  def init(): Unit = {
    registerRendering()
    registerTileEntities()
    registerTickHandlers()
  }

  def registerRendering() {
  }

  def registerTileEntities(): Unit = {
  }

  def registerTickHandlers() {
  }

  override def getServerGuiElement(ID: Int, data: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef = {
    (ID, world.getTileEntity(new BlockPos(x, y, z))) match {
      case (_, _) => null
    }
  }

  override def getClientGuiElement(ID: Int, data: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef = null

  def side: Side

  def addScheduledTask(f: () => Unit)

  def getWorld(id: Int): World

  def getIWorld(id: Int): IWorld

  def registerItemModel[T <: Item](item: ItemBuilder[T]): Unit = {
  }

  def registerBlockModel[B <: Block](block: BlockBuilder[B]): Unit = {

  }

}
