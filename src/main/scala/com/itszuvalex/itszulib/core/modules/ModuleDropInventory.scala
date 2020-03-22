package com.itszuvalex.itszulib.core.modules

import java.util.Random

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.{IModule, Loc4}
import com.itszuvalex.itszulib.api.wrappers.{IItemStack, ITileEntity}
import com.itszuvalex.itszulib.core.TileEntityInternalModule
import com.itszuvalex.itszulib.util.InventoryUtils
import net.minecraft.block.state.IBlockState

class ModuleDropInventory extends TileEntityInternalModule[ModuleDropInventory] {
  var shouldDrop: Boolean = true

  override def module: IModule[ModuleDropInventory] = InternalModules.MODULE_DROP_INVENTORY

  override def onBlockBreak(core: ITileEntity, state: IBlockState): Unit = {
    if (shouldDrop) {
      core.moduleOption(ItszuLibModules.ITEM_STORAGE, null).foreach { s =>
        val random = new Random
        s.foreach(InventoryUtils.dropItem(_, new Loc4(core), random))
        s.indices.foreach(s.setSlot(_, IItemStack.Empty))
      }
    }
  }
}
