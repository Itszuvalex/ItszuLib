package com.itszuvalex.itszulib.core.modules

import java.util.Random

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.{IModule, Loc4}
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.{IItemStack, ITileEntity}
import com.itszuvalex.itszulib.core.TileEntityModule
import com.itszuvalex.itszulib.util.InventoryUtils
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing

class ModuleDropInventory extends TileEntityModule {
  var shouldDrop: Boolean = true

  override def module: IModule[Nothing] = null

  override def faceToModuleMapper: EnumFacing => Option[Nothing] = null

  override def onBlockBreak(core: ITileEntity, state: IBlockState): Unit = {
    def dropAllInInv(i: IItemStorage): Unit = {
      val random = new Random
      i.foreach(InventoryUtils.dropItem(_, new Loc4(core), random))
      i.indices.foreach(i.setSlot(_, IItemStack.Empty))
    }

    if (shouldDrop) {
      core.getIWorld.getITileEntity(core.getPos) match {
        case null =>
        case ite: ITileEntity =>
          ite.moduleOption(ItszuLibModules.ITEM_STORAGE, null).foreach(dropAllInInv)
        case _ =>
      }
    }
  }
}
