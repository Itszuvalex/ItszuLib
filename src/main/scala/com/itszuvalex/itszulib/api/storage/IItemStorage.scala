package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.IItemCollectionAccess
import com.itszuvalex.itszulib.api.core.NBTSerializable
import net.minecraft.inventory.IInventory

/**
  * Created by Christopher Harris (Itszuvalex) on 3/13/2016.
  */
trait IItemStorage extends NBTSerializable {

  def getAccess: IItemCollectionAccess

  def getInventory: IInventory

}
