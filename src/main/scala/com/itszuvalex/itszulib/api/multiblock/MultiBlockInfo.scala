package com.itszuvalex.itszulib.api.multiblock

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.util.INBTSerializable

class MultiBlockInfo extends IMultiBlockComponent with INBTSerializable[NBTTagCompound] {
  private var isMultiBlock            = false
  private var controllerPos: BlockPos = new BlockPos(0, 0, 0)

  def isController(pos: BlockPos) = isValidMultiBlock && pos.equals(controllerPos)

  override def isValidMultiBlock = isMultiBlock

  override def formMultiBlock(world: World, pos: BlockPos): Boolean = {
    if (isMultiBlock) {
      if (pos != controllerPos) {
        return false
      }
    }
    isMultiBlock = true
    controllerPos = pos
    true
  }

  def cPos = controllerPos

  override def breakMultiBlock(world: World, pos: BlockPos): Boolean = {
    if (isMultiBlock) {
      if (pos != controllerPos) {
        return false
      }
    }
    isMultiBlock = false
    true
  }

  override def getInfo = this

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound
    nbt.setBoolean("isFormed", isMultiBlock)
    nbt.setInteger("c_x", controllerPos.getX)
    nbt.setInteger("c_y", controllerPos.getY)
    nbt.setInteger("c_z", controllerPos.getZ)
    nbt
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    isMultiBlock = nbt.getBoolean("isFormed")
    controllerPos = new BlockPos(nbt.getInteger("c_x"),
                                 nbt.getInteger("c_y"),
                                 nbt.getInteger("c_z"))
  }
}
