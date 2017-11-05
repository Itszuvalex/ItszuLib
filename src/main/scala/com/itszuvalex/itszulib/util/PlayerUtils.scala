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

import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.{BlockPos, RayTraceResult, Vec3d}
import net.minecraft.util.text.{TextComponentString, TextFormatting}
import net.minecraft.world.IBlockAccess
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

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

  /**
    * Determines a position which the player looks at, raytraced against blocks and a maximum range
    */
  @SideOnly(Side.CLIENT)
  def positionLookedAt(maxRange: Double, partialTicks: Float): Vec3d = {
    val player = Minecraft.getMinecraft.player
    player.rayTrace(maxRange, partialTicks) match {
      case hit if hit.typeOfHit == RayTraceResult.Type.BLOCK =>
        val world = player.getEntityWorld
        val hitPos = hit.getBlockPos
        val hitVec = hit.hitVec
        val block = world.getBlockState(hitPos).getBlock
        hit.sideHit match {
          case EnumFacing.DOWN => new Vec3d(hitVec.x, hitPos.getY - .001f, hitVec.z) // subtracted 1/1000 block to distinguish between hitting positive and negative sides
          case EnumFacing.UP => new Vec3d(hitVec.x, hitPos.getY + 1, hitVec.z)
          case EnumFacing.NORTH => new Vec3d(hitVec.x, hitVec.y, hitPos.getZ - .001f)
          case EnumFacing.SOUTH => new Vec3d(hitVec.x, hitVec.y, hitPos.getZ + 1)
          case EnumFacing.WEST => new Vec3d(hitPos.getX - .001f, hitVec.y, hitVec.z)
          case EnumFacing.EAST => new Vec3d(hitPos.getX + 1, hitVec.y, hitVec.z)
        }
      case _ => player.getPositionEyes(partialTicks).add(player.getLookVec.scale(maxRange))
    }
  }

  sealed trait BlockPassthroughMode {def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean}

  case object Replaceable extends BlockPassthroughMode {override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = block.isReplaceable(world, pos)}

  case object Passable extends BlockPassthroughMode {override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = block.isPassable(world, pos)}

  case object Air extends BlockPassthroughMode {override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = block.isAir(world.getBlockState(pos), world, pos)}

  case object None extends BlockPassthroughMode {override def canPass(block: Block, world: IBlockAccess, pos: BlockPos): Boolean = false}

}

