package com.itszuvalex.itszulib.proxy

import com.itszuvalex.itszulib.api.wrappers.{IWorld, WrapperWorld}
import net.minecraft.world.World
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.relauncher.Side

/**
  * Created by Christopher Harris (Itszuvalex) on 4/10/15.
  */
class ProxyServer extends ProxyCommon {
  override def side: Side = Side.SERVER

  override def addScheduledTask(f: () => Unit): Unit = FMLCommonHandler.instance().getMinecraftServerInstance.addScheduledTask(
    new Runnable {
      override def run() = f()
    })

  override def getWorld(id: Int): World = DimensionManager.getWorld(id)

  override def getIWorld(id: Int): IWorld = new WrapperWorld(DimensionManager.getWorld(id))
}
