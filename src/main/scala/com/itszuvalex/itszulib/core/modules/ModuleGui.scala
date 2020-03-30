package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.wrappers.{Converter, IWorld}
import com.itszuvalex.itszulib.core.{KeyedBoundingBox, TileEntityInternalModule}
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, EnumHand}

class ModuleGui(val mod: AnyRef, var guiId: () => Int) extends TileEntityInternalModule[ModuleGui] {
  override def module: IModule[ModuleGui] = InternalModules.MODULE_GUI

  override def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, box: Option[KeyedBoundingBox]): Boolean = {
    playerIn.openGui(mod, guiId(), Converter.WorldFromIWorld(world), pos.getX, pos.getY, pos.getZ)
    true
  }
}
