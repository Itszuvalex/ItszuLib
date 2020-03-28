package com.itszuvalex.itszulib.initialization

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}

class ModInit {
  val initializationManager: InitializationManager = new InitializationManager

  @EventHandler
  def preInit(event: FMLPreInitializationEvent): Unit = {
    MinecraftForge.EVENT_BUS.register(initializationManager)
    initializationManager.runInitStage(InitializationStage.Pre)
  }

  @EventHandler
  def load(event: FMLInitializationEvent): Unit = {
    initializationManager.runInitStage(InitializationStage.Main)
  }

  @EventHandler
  def postInit(event: FMLPostInitializationEvent): Unit = {
    initializationManager.runInitStage(InitializationStage.Post)
    MinecraftForge.EVENT_BUS.unregister(initializationManager)
  }
}
