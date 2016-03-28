package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.storage.IFluidCollectionStorage
import net.minecraft.entity.player.EntityPlayer

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/16.
  */
class StorageFluidCollectionAccess(private val storage: IFluidCollectionStorage) extends IFluidCollectionAccess {
  override def canPlayerAccess(player: EntityPlayer): Boolean = true

  override def length: Int = storage.length

  override def apply(idx: Int): IFluidAccess = new StorageFluidAccess(storage, idx)
}
