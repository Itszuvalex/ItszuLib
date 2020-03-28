package com.itszuvalex.itszulib.api.core

import net.minecraft.util.EnumFacing

trait IModuleProvider {
  def hasModule(mod: IModule[_], facing: EnumFacing): Boolean

  def getModule[T](mod: IModule[T], facing: EnumFacing): T
}
