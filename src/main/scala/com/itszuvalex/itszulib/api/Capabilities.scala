package com.itszuvalex.itszulib.api

import com.itszuvalex.itszulib.api.storage.{IItemStorage, ItemStorageArray}
import net.minecraft.nbt.{NBTBase, NBTTagCompound}
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.{Capability, CapabilityInject, CapabilityManager}

/**
  * Created by Chris on 7/31/2016.
  */
object Capabilities {
  @CapabilityInject(classOf[IItemStorage])
  val ITEM_STORAGE: Capability[IItemStorage] = null

  def register(): Unit = {
    CapabilityManager.INSTANCE.register(classOf[IItemStorage], new ItemStorageStorage, classOf[ItemStorageArray])
  }

  class ItemStorageStorage extends Capability.IStorage[IItemStorage] {
    override def writeNBT(capability: Capability[IItemStorage], instance: IItemStorage, side: EnumFacing): NBTBase = instance.serializeNBT()

    override def readNBT(capability: Capability[IItemStorage], instance: IItemStorage, side: EnumFacing, nbt: NBTBase): Unit = instance.deserializeNBT(nbt.asInstanceOf[NBTTagCompound])
  }

}
