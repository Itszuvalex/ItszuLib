package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.storage.IPowerCollectionStorage
import net.minecraft.entity.player.EntityPlayer

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
class StoragePowerCollectionAccess(private val storage: IPowerCollectionStorage) extends IPowerCollectionAccess {
  override def canPlayerAccess(player: EntityPlayer): Boolean = true

  override def length: Int = storage.length

  override def apply(idx: Int): IPowerAccess = new StoragePowerAccess(storage, idx)

  override def getRevision: Int = storage.getRevision

  override def incrementRevision(): Int = storage.incrementRevision()
}
