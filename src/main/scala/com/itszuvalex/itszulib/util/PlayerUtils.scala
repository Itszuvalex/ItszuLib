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
package com.itszuvalex.itszulib.util

import java.util.UUID

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.util.text.{TextComponentString, TextFormatting}
import net.minecraftforge.fml.common.FMLCommonHandler

object PlayerUtils {
  /**
    *
    * @param username
    *
    * @return True if MinecraftServer sees the player as online.
    */
  def isPlayerOnline(username: String) =
    Option(FMLCommonHandler.instance().getMinecraftServerInstance).exists(_.getOnlinePlayerNames.contains(username))


  /**
    *
    * @param username
    *
    * @return The player entity of the player with username.
    */
  def getLocalPlayer(username: String): EntityPlayer = Minecraft.getMinecraft.world.getPlayerEntityByName(username)

  //  def getServerPlayer(username: String): EntityPlayerMP = MinecraftServer.getServer.getConfigurationManager.func_152612_a(username)

  def getServerPlayer(uuid: UUID): EntityPlayerMP = FMLCommonHandler.instance().getMinecraftServerInstance.getPlayerList.getPlayerByUUID(uuid)

  def sendMessageToPlayer(username: String, modID: String, message: String): Boolean = sendMessageToPlayer(username, modID, message, "")

  def sendMessageToPlayer(username: String, modID: String, message: String,
    formatting: String): Boolean = sendMessageToPlayer(FMLCommonHandler
    .instance()
    .getMinecraftServerInstance
    .getPlayerList
    .getPlayerByUsername(username),
    modID,
    message,
    formatting)

  /**
    *
    * Sends chat message "(GOLD)[Femtocraft](RESET): (formatting)(message)(RESET)"
    * to player.
    *
    * @param player     player to send message to
    * @param message    Message to send to player
    * @param formatting Any formatting you wish to apply to the message as a whole.
    *
    * @return True if player exists and message sent.
    */
  def sendMessageToPlayer(player: EntityPlayer, modID: String, message: String, formatting: String): Boolean = {
    if (player != null) {
      player
        .sendMessage(new TextComponentString(new StringBuilder()
          .append(TextFormatting.GOLD)
          .append(modID)
          .append(TextFormatting.RESET)
          .append(": ")
          .append(formatting)
          .append(message)
          .append(TextFormatting.RESET)
          .toString()))
      return true
    }
    false
  }

  def sendMessageToPlayer(player: EntityPlayer, modID: String, message: String): Boolean = sendMessageToPlayer(player,
    modID,
    message,
    "")

}

