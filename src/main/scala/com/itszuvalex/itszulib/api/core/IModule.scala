package com.itszuvalex.itszulib.api.core
import net.minecraftforge.common.capabilities.Capability

trait IModule[T] {

  def hasCapability: Boolean

  def capability: Capability[T]

  def name: String
}
