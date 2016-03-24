package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.storage.IItemStorage
import net.minecraft.entity.player.EntityPlayer

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
class StorageItemCollectionAccess(private val storage: IItemStorage) extends IItemCollectionAccess {
  override def canPlayerAccess(player: EntityPlayer): Boolean = true

  override def length: Int = storage.getSize

  override def apply(idx: Int): IItemAccess = new StorageItemAccess(storage, idx)

  override def getRevision: Int = storage.getRevision

  override def incrementRevision(): Int = storage.incrementRevision()
}
