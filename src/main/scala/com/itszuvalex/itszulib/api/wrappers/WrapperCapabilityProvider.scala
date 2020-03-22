package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.core.{IModule, IModuleProvider}
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.ICapabilityProvider

class WrapperCapabilityProvider(val cp: ICapabilityProvider) extends IModuleProvider {
  override def hasModule(mod: IModule[_], facing: EnumFacing): Boolean = if (mod.hasCapability)
    cp.hasCapability(mod.capability, facing)
  else false

  override def getModule[T](mod: IModule[T], facing: EnumFacing): T = if (mod.hasCapability)
    cp.getCapability(mod.capability, facing)
  else null.asInstanceOf[T]
}
