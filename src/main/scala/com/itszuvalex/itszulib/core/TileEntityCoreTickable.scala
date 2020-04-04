package com.itszuvalex.itszulib.core

import net.minecraft.util.ITickable
import net.minecraftforge.fml.server.FMLServerHandler

import scala.collection.mutable

abstract class TileEntityCoreTickable extends TileEntityCore with ITickable {
  val modulesTickable = new mutable.ArrayBuffer[ITileEntityModuleTickable[_]]()

  def addTileEntityModuleTickable[T](mod: ITileEntityModuleTickable[T]): Unit = {
    modulesTickable += mod
    addTileEntityModule(mod)
  }

  override def update(): Unit = {
    // Server update on the server Worlds
    if (!getIWorld.isRemote)
      serverUpdate()
    //
    if (getIWorld.isRemote || Option(FMLServerHandler.instance().getServer).forall(!_.isDedicatedServer))
        clientUpdate()
  }

  /**
    * Gated update call. This will only be called on the server. This should be used instead of updateEntity() for heavy computation, unless the tile absolutely needs to
    * update.
    */
  def serverUpdate(): Unit = {
    modulesTickable.view.foreach(_.serverUpdate(this))
  }

  /**
    * Gated update call.  This will only be called on the client.  This should be used instead of updateEntity() for client-side only things like rendering/sounds.
    */
  def clientUpdate(): Unit = {
    modulesTickable.view.foreach(_.clientUpdate(this))
  }
}
