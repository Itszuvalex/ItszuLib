package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.core.IModule
import com.itszuvalex.itszulib.api.multiblock.MultiBlockInfo
import com.itszuvalex.itszulib.api.wrappers.{Converter, ITileEntity, IWorld}
import com.itszuvalex.itszulib.core.{KeyedBoundingBox, TileEntityInternalModule}
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, EnumHand}

class ModuleMultiblockGui(val info: MultiBlockInfo, val mod: AnyRef, var guiId: () => Int) extends TileEntityInternalModule[ModuleMultiblockGui] {
  override def module: IModule[ModuleMultiblockGui] = InternalModules.MODULE_MULTIBLOCK_GUI

  override def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, box: Option[KeyedBoundingBox]): Boolean = {
    if (info.isValidMultiBlock) {
      if (info.isController) {
        playerIn.openGui(mod, guiId(), Converter.WorldFromIWorld(world), pos.getX, pos.getY, pos.getZ)
        true
      }
      else {
        info.controller.flatMap(_.getITileEntity(true)) match {
          case None => false
          case Some(tile: ITileEntity) =>
            tile.moduleOption(InternalModules.MODULE_MULTIBLOCK_GUI, null).exists(_.onBlockActivated(world, tile.getPos, tile.getIWorld.getBlockState(tile.getPos), playerIn, hand, facing, hitX, hitY, hitZ, box))
          case _ => false
        }
      }
    }
    else false
  }
}
