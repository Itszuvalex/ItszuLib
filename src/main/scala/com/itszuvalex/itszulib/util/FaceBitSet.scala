package com.itszuvalex.itszulib.util

import net.minecraft.nbt.NBTTagByte
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.INBTSerializable

/**
  * Implements an [[EnumFacing]] -> [[Boolean]] BitSet-ish backed by a Byte.
  * Each bit for an [[EnumFacing]] is packed at 1 << EnumFacing.Values.getIndex
  * Supports null [[EnumFacing]].  Stored at getIndex == 7
  */
class FaceBitSet extends INBTSerializable[NBTTagByte] {
  private var bitset: Byte = 0

  def get(face: EnumFacing): Boolean = {
    ((bitset >> indexOfFace(face)) & 1.toByte) > 0
  }

  def set(face: EnumFacing): Unit = {
    bitset = (bitset | (1.toByte << indexOfFace(face))).toByte
  }

  def clear(face: EnumFacing): Unit = {
    bitset = (bitset & ~(1.toByte << indexOfFace(face))).toByte
  }

  def toggle(face: EnumFacing): Unit = {
    bitset = (bitset ^ (1.toByte << indexOfFace(face))).toByte
  }

  def setTo(face: EnumFacing, boolean: Boolean): Unit = if (boolean) {
    set(face)
  } else {
    clear(face)
  }

  private def indexOfFace(face: EnumFacing): Byte =
    (face match {
      case null => EnumFacing.VALUES.length + 1
      case i => i.getIndex
    }).toByte

  override def serializeNBT(): NBTTagByte = new NBTTagByte(bitset)

  override def deserializeNBT(nbt: NBTTagByte): Unit = bitset = nbt.getByte
}
