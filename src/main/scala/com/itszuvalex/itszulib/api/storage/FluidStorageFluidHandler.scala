package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.{Converter, IFluidStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.capability.IFluidHandler

class FluidStorageFluidHandler(private val handler: IFluidHandler) extends IFluidStorage {
  override def deserializeNBT(nbt: NBTTagCompound): Unit = {}

  override def serializeNBT(): NBTTagCompound = new NBTTagCompound

  override def fill(resource: IFluidStack, doFill: Boolean): Int = handler.fill(resource.toMinecraft, doFill)

  override def drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = Converter.IFluidStackFromFluidStack(handler.drain(resource.toMinecraft, doDrain))

  override def drainIStack(maxDrain: mL, doDrain: Boolean): IFluidStack = Converter.IFluidStackFromFluidStack(handler.drain(maxDrain, doDrain))

  override def length: Int = handler.getTankProperties.length

  override def apply(idx: Int): IFluidStack = Converter.IFluidStackFromFluidStack(handler.getTankProperties()(idx).getContents)
}
