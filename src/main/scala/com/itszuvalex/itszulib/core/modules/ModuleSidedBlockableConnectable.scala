package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.util.FaceBitSet
import net.minecraft.nbt.{NBTTagByte, NBTTagCompound}
import net.minecraft.util.EnumFacing

object ModuleSidedBlockableConnectable {
  val BLOCK_NBT = "Block"
}

abstract class ModuleSidedBlockableConnectable[T] extends ModuleSidedConnectable[T] {
  val blocked = new FaceBitSet

  def isBlocked(face: EnumFacing): Boolean = face match {
    case null => true
    case f => blocked.get(f)
  }

  /**
    *
    * @param face
    * @return True if successfully added connection.  False if blocked or face is null
    */
  override def connect(face: EnumFacing): Boolean = face match {
    case null => false
    case f if blocked.get(f) => false
    case c => super.connect(c)
  }

  /**
    *
    * @param face
    * @return True if successfullly removed connection.  False if already blocked.  False if face is null
    */
  override def disconnect(face: EnumFacing): Boolean = face match {
    case null => false
    case f if blocked.get(f) => false
    case c => super.disconnect(face)
      true
  }

  /**
    *
    * @param face
    * @return Returns the connected status (true/false) before being blocked. Connected will be set to false regardless.
    *         Or false if face == null
    */
  def block(face: EnumFacing): Boolean = face match {
    case null => false
    case f =>
      val ret = connections.get(f)
      connections.clear(f)
      blocked.set(f)
      ret
  }

  /**
    *
    * @param face
    * @return False only if face == null
    */
  def unblock(face: EnumFacing): Boolean = face match {
    case null => false
    case f =>
      blocked.clear(f)
      true
  }

  override def writeDescriptionNBT(tag: NBTTagCompound): Unit = {
    super.writeDescriptionNBT(tag)
    writeBlockNBT(tag)
  }

  override def readDescriptionNBT(tag: NBTTagCompound): Unit = {
    super.readDescriptionNBT(tag)
    readBlockNBT(tag)
  }

  override def writeWorldNBT(tag: NBTTagCompound): Unit = {
    super.writeWorldNBT(tag)
    writeBlockNBT(tag)
  }

  override def readWorldNBT(tagCompound: NBTTagCompound): Unit = {
    super.readWorldNBT(tagCompound)
    readBlockNBT(tagCompound)
  }

  def writeBlockNBT(tag: NBTTagCompound): Unit = {
    tag.setTag(ModuleSidedBlockableConnectable.BLOCK_NBT, blocked.serializeNBT())
  }

  def readBlockNBT(tag: NBTTagCompound): Unit = {
    blocked.deserializeNBT(tag.getTag(ModuleSidedBlockableConnectable.BLOCK_NBT).asInstanceOf[NBTTagByte])
  }
}
