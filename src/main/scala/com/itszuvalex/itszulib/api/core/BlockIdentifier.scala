package com.itszuvalex.itszulib.api.core

import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.util.INBTSerializable

object BlockIdentifier {
  def apply(nbt: NBTTagCompound): BlockIdentifier = {
    val bi = BlockIdentifier("", "")
    bi.deserializeNBT(nbt)
    bi
  }

  def apply(block: Block): BlockIdentifier =
    BlockIdentifier(
      block.getRegistryName.getResourceDomain,
      block.getRegistryName.getResourcePath)

  val NBT_MOD_ID_KEY   = "m"
  val NBT_BLOCK_ID_KEY = "b"
}

case class BlockIdentifier(var modId: String, var blockId: String) extends INBTSerializable[NBTTagCompound] {

  def block: Block = Block.REGISTRY.getObject(resourceLocation)

  def resourceLocation: ResourceLocation = new ResourceLocation(modId, blockId)

  override def serializeNBT(): NBTTagCompound = {
    val ret = new NBTTagCompound
    ret.setString(BlockIdentifier.NBT_MOD_ID_KEY, modId)
    ret.setString(BlockIdentifier.NBT_BLOCK_ID_KEY, blockId)
    ret
  }

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    modId = nbt.getString(BlockIdentifier.NBT_MOD_ID_KEY)
    blockId = nbt.getString(BlockIdentifier.NBT_BLOCK_ID_KEY)
  }
}
