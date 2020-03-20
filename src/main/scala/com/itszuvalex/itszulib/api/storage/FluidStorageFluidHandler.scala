package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.{Converter, IFluidStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.capability.IFluidHandler

class FluidStorageFluidHandler(private val handler: IFluidHandler) extends IFluidStorage {
  override def deserializeNBT(nbt: NBTTagCompound): Unit = {}

  override def serializeNBT(): NBTTagCompound = new NBTTagCompound

  override def fill(resource: IFluidStack, doFill: Boolean): Int = handler.fill(resource.toMinecraft, doFill)

  override def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = Converter.IFluidStackFromFluidStack(handler.drain(resource.toMinecraft, doDrain))

  override def drainIStack(maxDrain: Int, doDrain: Boolean): IFluidStack = Converter.IFluidStackFromFluidStack(handler.drain(maxDrain, doDrain))

  override def contents: IFluidStack = {
    val props = handler.getTankProperties
    var c     = IFluidStack.Empty
    props.exists { t =>
      c = Converter.IFluidStackFromFluidStack(t.getContents)
      true
    }
    c
  }

  override def capacity: Int = {
    val props = handler.getTankProperties
    var cap   = 0
    props.exists { t =>
      cap = t.getCapacity
      true
    }
    cap
  }

  override def canFillFluidType(resource: IFluidStack): Boolean = handler.getTankProperties.exists(_.canFillFluidType(Converter.FluidStackFromIFluidStack(resource)))

  override def canDrainFluidType(resource: IFluidStack): Boolean = handler.getTankProperties.exists(_.canDrainFluidType(Converter.FluidStackFromIFluidStack(resource)))

  override def canDrain: Boolean = handler.getTankProperties.exists(_.canDrain)

  override def canFill: Boolean = handler.getTankProperties.exists(_.canFill)
}
