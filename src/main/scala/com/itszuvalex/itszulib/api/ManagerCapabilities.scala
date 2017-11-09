package com.itszuvalex.itszulib.api

import com.itszuvalex.itszulib.api.storage.{IItemStorage, ItemStorageArray}
import net.minecraft.nbt.{NBTBase, NBTTagCompound}
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.{Capability, CapabilityManager}

/**
  * Created by Chris on 7/31/2016.
  */
object ManagerCapabilities {

  def register(): Unit = {
    CapabilityManager.INSTANCE.register(classOf[IItemStorage], new ItemStorageStorage, classOf[ItemStorageArray])
    CapabilityManager.INSTANCE.register(classOf[IPreviewable], new ItemPreviewableStorage, classOf[ItemPreviewableImpl])

    MinecraftForge.EVENT_BUS.register(this)
  }

  class ItemStorageStorage extends Capability.IStorage[IItemStorage] {
    override def writeNBT(capability: Capability[IItemStorage], instance: IItemStorage, side: EnumFacing): NBTBase = instance.serializeNBT()

    override def readNBT(capability: Capability[IItemStorage], instance: IItemStorage, side: EnumFacing, nbt: NBTBase): Unit = instance.deserializeNBT(nbt.asInstanceOf[NBTTagCompound])
  }

  class ItemPreviewableStorage extends Capability.IStorage[IPreviewable] {
    override def writeNBT(capability: Capability[IPreviewable], instance: IPreviewable, side: EnumFacing): NBTBase = new NBTTagCompound

    override def readNBT(capability: Capability[IPreviewable], instance: IPreviewable, side: EnumFacing, nbt: NBTBase): Unit = {}
  }

  class ItemPreviewableImpl extends IPreviewable {
    /**
      *
      * @return The ID of IPreviewableRenderer.  This is separate from Forge RenderIDs.
      */
    override def renderID: Int = 0
  }

}
