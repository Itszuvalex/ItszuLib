package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.IAccess
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/16.
  */
trait IStorage[T <: IStorage[T, A, D], A <: IAccess[A, D], D] extends INBTSerializable[NBTTagCompound] {

  def getAccess: A

  def onChanged(): Unit = {}
}
