package com.itszuvalex.itszulib.gui

import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

import scala.collection.mutable

/**
  * Created by Christopher Harris (Itszuvalex) on 2/1/2016.
  */
@SideOnly(Side.CLIENT)
object GuiStack {
  val guiStack = new mutable.Stack[GuiBase]()

  def popStack() = {
    val gui = if (guiStack.isEmpty) null else guiStack.pop()
    Minecraft.getMinecraft.displayGuiScreen(gui)
  }

  def pushStack(gui: GuiBase) = guiStack.push(gui)

  def init(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @EventHandler
  def handleScreen(event: net.minecraftforge.client.event.GuiOpenEvent): Unit = {
    if (event.getGui == null)
      clearStack()
    GuiTextBox.activeTextBox = null // Don't carry over focus between GUIs
  }

  def clearStack() = guiStack.clear()

}
