package com.itszuvalex.itszulib.api.core

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.fluids.{Fluid, FluidRegistry}

object FluidIdentifier {
  def apply(nbt: NBTTagCompound): FluidIdentifier = {
    val bi = FluidIdentifier("")
    bi.deserializeNBT(nbt)
    bi
  }

  def apply(fluid: Fluid): FluidIdentifier =
    FluidIdentifier(FluidRegistry.getFluidName(fluid))

  val NBT_KEY = "f"
}

case class FluidIdentifier(var name: String) extends INBTSerializable[NBTTagCompound] {

  def fluid: Fluid = FluidRegistry.getFluid(name)

  def resourceLocation: ResourceLocation = new ResourceLocation(name)

  override def serializeNBT(): NBTTagCompound = {
    val ret = new NBTTagCompound
    ret.setString(FluidIdentifier.NBT_KEY, name)
    ret
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    name = nbt.getString(FluidIdentifier.NBT_KEY)
  }
}
