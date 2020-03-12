package com.itszuvalex.itszulib.api.core

import net.minecraftforge.common.capabilities.Capability

class Module[T](val capabilityGetter: () => Capability[T]) {
  def this() = this(null)

  def hasCapability: Boolean = capabilityGetter != null

  def capability: Capability[T] = capabilityGetter()
}
