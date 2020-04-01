package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.core.TileEntityModule
import com.itszuvalex.itszulib.util.FaceBitSet
import net.minecraft.nbt.{NBTTagByte, NBTTagCompound}
import net.minecraft.util.EnumFacing

object ModuleSidedConnectable {
  val CON_NBT = "Con"
}

abstract class ModuleSidedConnectable[T <: ModuleSidedConnectable[T]] extends TileEntityModule[T] {
  val connections = new FaceBitSet

  def isConnected(face: EnumFacing): Boolean = face match {
    case null => false
    case f => connections.get(f)
  }

  /**
    *
    * @param face
    * @return True if successfully added connection. False if face is null
    */
  def connect(face: EnumFacing): Boolean = face match {
    case null => false
    case c =>
      connections.set(c)
      true
  }

  /**
    *
    * @param face
    * @return True if successfullly removed connection. False if face is null
    */
  def disconnect(face: EnumFacing): Boolean = face match {
    case null => false
    case c =>
      connections.clear(c)
      true
  }

  override def hasDescriptionNBT: Boolean = true

  override def hasWorldNBT: Boolean = true

  override def writeDescriptionNBT(tag: NBTTagCompound): Unit = {writeConNBT(tag)}

  override def readDescriptionNBT(tag: NBTTagCompound): Unit = {readConNBT(tag)}

  override def writeWorldNBT(tag: NBTTagCompound): Unit = {writeConNBT(tag)}

  override def readWorldNBT(tagCompound: NBTTagCompound): Unit = {readConNBT(tagCompound)}

  def writeConNBT(tag: NBTTagCompound): Unit = {
    tag.setTag(ModuleSidedConnectable.CON_NBT, connections.serializeNBT())
  }

  def readConNBT(tag: NBTTagCompound): Unit = {
    connections.deserializeNBT(tag.getTag(ModuleSidedConnectable.CON_NBT).asInstanceOf[NBTTagByte])
  }
}
