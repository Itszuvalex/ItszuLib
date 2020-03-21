package com.itszuvalex.itszulib.api.core

import net.minecraftforge.common.capabilities.Capability

import scala.collection.mutable

object Module {
  val modules = new mutable.HashMap[String, IModule[_]]()

  def registerModule[T](name: String, capabilityGetter: () => Capability[T]): IModule[T] = {
    if (modules.contains(name))
      throw new IllegalArgumentException(s"Module with name: $name already registered.")
    val m = new Module(name, capabilityGetter)
    modules(name) = m
    m
  }

  /**
    * Test only, do not use while Minecraft is actually running.
    */
  def clear(): Unit = {
    modules.clear()
  }
}

private class Module[T](val n: String, val capabilityGetter: () => Capability[T]) extends IModule[T] {

  override def name: String = n

  override def hasCapability: Boolean = capabilityGetter != null

  override def capability: Capability[T] = capabilityGetter()
}
