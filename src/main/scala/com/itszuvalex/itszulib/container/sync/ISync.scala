package com.itszuvalex.itszulib.container.sync

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTBase

/**
  * Created by Chris on 12/13/2016.
  */
trait ISync[A] {
  def index: Int

  def index_=(i: Int): Unit

  def value: A

  def value_=(a: A): Unit

  /**
    * Updates the cached value.
    *
    * @return True if should sync to players.
    */
  def update(): Boolean

  def sync(player: EntityPlayer): Unit

  def writeNBT(): NBTBase

  def handleNBT(nbt: NBTBase): Unit
}
