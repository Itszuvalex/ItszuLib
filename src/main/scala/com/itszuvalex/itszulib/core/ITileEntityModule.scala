package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.wrappers.{IItemStack, ITileEntity, IWorld}
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

trait ITileEntityModule[T] {

  def name: String = module.name

  def module: IModule[T]

  def faceToModuleMapper: EnumFacing => Option[T]

  def hasItemNBT: Boolean

  def hasDescriptionNBT: Boolean

  def hasWorldNBT: Boolean

  def writeItemNBT(tag: NBTTagCompound): Unit

  def readItemNBT(tag: NBTTagCompound): Unit

  def writeDescriptionNBT(tag: NBTTagCompound): Unit

  def readDescriptionNBT(tag: NBTTagCompound): Unit

  def writeWorldNBT(tag: NBTTagCompound): Unit

  def readWorldNBT(tagCompound: NBTTagCompound): Unit

  def invalidate(tile: ITileEntity): Unit

  def validate(tile: ITileEntity): Unit

  def onChunkUnload(tile: ITileEntity): Unit

  def onLoad(tile: ITileEntity): Unit

  def onBlockBreak(core: ITileEntity, state: IBlockState): Unit

  def onBlockPlacedBy(iworld: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, istack: IItemStack): Unit

}
