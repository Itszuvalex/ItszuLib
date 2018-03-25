package com.itszuvalex.itszulib.api

import com.itszuvalex.itszulib.api.multiblock.MultiBlockInfo
import com.itszuvalex.itszulib.api.storage.{IFluidStorage, IItemStorage, ItemStorageArray}
import com.itszuvalex.itszulib.util.Color
import net.minecraft.nbt.{NBTBase, NBTTagCompound, NBTTagInt}
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
    CapabilityManager.INSTANCE.register(classOf[IFluidStorage], new FluidStorageStorage, classOf[IFluidStorage])
    CapabilityManager.INSTANCE.register(classOf[Color], new ColorStorage, classOf[Color])
    CapabilityManager.INSTANCE.register(classOf[MultiBlockInfo], new MultiBlockInfoStorage, classOf[MultiBlockInfo])

    MinecraftForge.EVENT_BUS.register(this)
  }

  class ColorStorage extends Capability.IStorage[Color] {
    override def writeNBT(capability: Capability[Color], instance: Color, side: EnumFacing): NBTBase = new NBTTagInt(instance.toInt)

    override def readNBT(capability: Capability[Color], instance: Color, side: EnumFacing, nbt: NBTBase): Unit = {
      val copy = new Color(nbt.asInstanceOf[NBTTagInt].getInt)
      instance.red = copy.red
      instance.green = copy.green
      instance.blue = copy.blue
      instance.alpha = copy.alpha
    }
  }

  class MultiBlockInfoStorage extends Capability.IStorage[MultiBlockInfo] {
    override def readNBT(capability: Capability[MultiBlockInfo], instance: MultiBlockInfo, side: EnumFacing, nbt: NBTBase): Unit =
      instance.deserializeNBT(nbt.asInstanceOf[NBTTagCompound])

    override def writeNBT(capability: Capability[MultiBlockInfo], instance: MultiBlockInfo, side: EnumFacing): NBTBase =
      instance.serializeNBT()
  }

  abstract class DummyStorage[T] extends Capability.IStorage[T] {
    override def writeNBT(capability: Capability[T], instance: T, side: EnumFacing): NBTBase = {new NBTTagCompound}

    override def readNBT(capability: Capability[T], instance: T, side: EnumFacing, nbt: NBTBase): Unit = {}
  }

  class ItemStorageStorage extends Capability.IStorage[IItemStorage] {
    override def writeNBT(capability: Capability[IItemStorage], instance: IItemStorage, side: EnumFacing): NBTBase = instance.serializeNBT()

    override def readNBT(capability: Capability[IItemStorage], instance: IItemStorage, side: EnumFacing, nbt: NBTBase): Unit = instance.deserializeNBT(nbt.asInstanceOf[NBTTagCompound])
  }

  class FluidStorageStorage extends Capability.IStorage[IFluidStorage] {
    override def readNBT(capability: Capability[IFluidStorage], instance: IFluidStorage, side: EnumFacing, nbt: NBTBase): Unit = instance.deserializeNBT(nbt.asInstanceOf[NBTTagCompound])

    override def writeNBT(capability: Capability[IFluidStorage], instance: IFluidStorage, side: EnumFacing): NBTBase = instance.serializeNBT()
  }

  class ItemPreviewableStorage extends DummyStorage[IPreviewable]

  class ItemPreviewableImpl extends IPreviewable {
    /**
      *
      * @return The ID of IPreviewableRenderer.  This is separate from Forge RenderIDs.
      */
    override def renderID: Int = 0
  }

}
