package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.wrappers.ITileEntity
import com.itszuvalex.itszulib.core.TileEntityModule
import com.itszuvalex.itszulib.util.Color
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

object ModuleColorable {
  val COLOR_NBT = "Color"
}

class ModuleColorable extends TileEntityModule[Color] {
  var color: Color = Color(0, 0, 0, 0)

  override def module: IModule[Color] = ItszuLibModules.COLORABLE

  override def faceToModuleMapper(tile: ITileEntity): EnumFacing => Option[Color] = _ => Some(color)

  override def hasDescriptionNBT: Boolean = true

  override def hasWorldNBT: Boolean = true

  override def writeDescriptionNBT(tag: NBTTagCompound): Unit = saveToNBT(tag)

  override def readDescriptionNBT(tag: NBTTagCompound): Unit = readFromNBT(tag)

  override def writeWorldNBT(tag: NBTTagCompound): Unit = saveToNBT(tag)

  override def readWorldNBT(tagCompound: NBTTagCompound): Unit = readFromNBT(tagCompound)

  def saveToNBT(tag: NBTTagCompound): Unit = {
    tag.setInteger(ModuleColorable.COLOR_NBT, color.toInt)
  }

  def readFromNBT(tag: NBTTagCompound): Unit = {
    color = new Color(tag.getInteger(ModuleColorable.COLOR_NBT))
  }
}
