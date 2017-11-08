package com.itszuvalex.itszulib.api

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.storage.{IItemStorage, ItemStorageArray}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTBase, NBTTagCompound}
import net.minecraft.util.{EnumFacing, ResourceLocation}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.{Capability, CapabilityManager}
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
  * Created by Chris on 7/31/2016.
  */
object ManagerCapabilities {

  def register(): Unit = {
    CapabilityManager.INSTANCE.register(classOf[IItemStorage], new ItemStorageStorage, classOf[ItemStorageArray])
    CapabilityManager.INSTANCE.register(classOf[IBurnable], new ItemBurnableStorage, classOf[ItemBurnableImpl])
    CapabilityManager.INSTANCE.register(classOf[IPreviewable], new ItemPreviewableStorage, classOf[ItemPreviewableImpl])

    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent
  def gatherCapabilities(event: AttachCapabilitiesEvent[ItemStack]): Unit = {
    IBurnable.getProvider(event.getObject) match {
      case Some(a) => event.addCapability(new ResourceLocation(ItszuLib.ID.toLowerCase, "IBurnable"), a)
      case _ =>
    }
  }

  class ItemStorageStorage extends Capability.IStorage[IItemStorage] {
    override def writeNBT(capability: Capability[IItemStorage], instance: IItemStorage, side: EnumFacing): NBTBase = instance.serializeNBT()

    override def readNBT(capability: Capability[IItemStorage], instance: IItemStorage, side: EnumFacing, nbt: NBTBase): Unit = instance.deserializeNBT(nbt.asInstanceOf[NBTTagCompound])
  }

  class ItemBurnableStorage extends Capability.IStorage[IBurnable] {
    override def writeNBT(capability: Capability[IBurnable], instance: IBurnable, side: EnumFacing): NBTBase = new NBTTagCompound

    override def readNBT(capability: Capability[IBurnable], instance: IBurnable, side: EnumFacing, nbt: NBTBase): Unit = {}
  }

  class ItemPreviewableStorage extends Capability.IStorage[IPreviewable] {
    override def writeNBT(capability: Capability[IPreviewable], instance: IPreviewable, side: EnumFacing): NBTBase = new NBTTagCompound

    override def readNBT(capability: Capability[IPreviewable], instance: IPreviewable, side: EnumFacing, nbt: NBTBase): Unit = {}
  }

  class ItemBurnableImpl extends IBurnable {
    override def getBurnTime: Int = 0
  }

  class ItemPreviewableImpl extends IPreviewable {
    /**
      *
      * @return The ID of IPreviewableRenderer.  This is separate from Forge RenderIDs.
      */
    override def renderID: Int = 0
  }

}
