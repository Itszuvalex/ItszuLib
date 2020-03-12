package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.core.Module
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

import scala.collection.mutable

class ModuleCapabilityMap {
  private val modMap = new mutable.HashMap[Module[_], EnumFacing => Option[_]]()
  private val capMap = new mutable.HashMap[Capability[_], EnumFacing => Option[_]]()

  def addModule[T <: Any](mod: Module[T], func: EnumFacing => Option[T]): Unit = {
    modMap(mod) = func
    if (mod.hasCapability) {
      val cap = mod.capability
      if (cap != null) {
        capMap(mod.capability) = func
      }
    }
  }

  def hasModule(mod: Module[_], facing: EnumFacing): Boolean = {
    modMap.get(mod).exists(_.apply(facing).isDefined)
  }

  def getModule[T <: Any](mod: Module[T], facing: EnumFacing): T = {
    modMap.get(mod).flatMap(_.apply(facing)).orNull.asInstanceOf[T]
  }

  def moduleOption[T <: Any](mod: Module[T], facing: EnumFacing): Option[T] = {
    Option(getModule(mod, facing))
  }

  def addCapability[T <: Any](cap: Capability[T], func: EnumFacing => Option[T]): Unit = {
    capMap(cap) = func
  }

  def hasCapability(cap: Capability[_], facing: EnumFacing): Boolean = {
    capMap.get(cap).exists(_.apply(facing).isDefined)
  }

  def getCapability[T <: Any](cap: Capability[T], facing: EnumFacing): T = {
    capMap.get(cap).flatMap(_.apply(facing)).orNull.asInstanceOf[T]
  }

  def capabilityOption[T <: Any](cap: Capability[T], facing: EnumFacing): Option[T] = {
    Option(getCapability(cap, facing))
  }

}
