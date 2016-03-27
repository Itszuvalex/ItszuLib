package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.storage.IFluidTankStorage
import net.minecraft.entity.player.EntityPlayer

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/16.
  */
class StorageFluidTankCollectionAccess(private val storage: IFluidTankStorage) extends IFluidTankCollectionAccess {
  override def canPlayerAccess(player: EntityPlayer): Boolean = true

  override def length: Int = storage.length

  override def apply(idx: Int): IFluidTankAccess = new StorageFluidTankAccess(storage, idx)
}
