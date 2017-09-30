package com.itszuvalex.itszulib.gui

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler

object ItszuGuiHandler {

  /**
    * Opens a gui with additional data passed to it.
    *
    * @param player The player
    * @param mod
    * @param ID
    * @param data
    * @param world
    * @param x
    * @param y
    * @param z
    */
  def openGui(player: EntityPlayer, mod: AnyRef, ID: Int, data: Int, world: World, x: Int, y: Int, z: Int): Unit = {
    val compID = ((data & 0xFFFF) << 16) | (ID & 0xFFFF)
    FMLNetworkHandler.openGui(player, mod, compID, world, x, y, z)
  }

}

trait ItszuGuiHandler extends IGuiHandler {
  /**
    * Returns a Server side Container to be displayed to the user.
    *
    * @param ID     The Gui ID Number
    * @param data   Additional GUI data passed by the openGui call
    * @param player The player viewing the Gui
    * @param world  The current world
    * @param x      X Position
    * @param y      Y Position
    * @param z      Z Position
    * @return A GuiScreen/Container to be displayed to the user, null if none.
    */
  def getServerGuiElement(ID: Int, data: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef

  override def getServerGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef = {
    val data = (ID & 0xFFFF0000) >> 16
    val actualID = ID & 0xFFFF
    getServerGuiElement(actualID, data, player, world, x, y, z)
  }

  /**
    * Returns a Container to be displayed to the user. On the client side, this
    * needs to return a instance of GuiScreen On the server side, this needs to
    * return a instance of Container
    *
    * @param ID     The Gui ID Number
    * @param data   Additional GUI data passed by the openGui call
    * @param player The player viewing the Gui
    * @param world  The current world
    * @param x      X Position
    * @param y      Y Position
    * @param z      Z Position
    * @return A GuiScreen/Container to be displayed to the user, null if none.
    */
  def getClientGuiElement(ID: Int, data: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef

  override def getClientGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef = {
    val data = (ID & 0xFFFF0000) >> 16
    val actualID = ID & 0xFFFF
    getClientGuiElement(actualID, data, player, world, x, y, z)
  }

}
