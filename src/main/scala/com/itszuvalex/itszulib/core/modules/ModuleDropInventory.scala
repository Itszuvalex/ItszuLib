package com.itszuvalex.itszulib.core.modules

import java.util.Random

import com.itszuvalex.itszulib.api.core.{IModule, Loc4}
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.{IItemStack, ITileEntity}
import com.itszuvalex.itszulib.core.TileEntityInternalModule
import com.itszuvalex.itszulib.util.InventoryUtils
import net.minecraft.block.state.IBlockState

class ModuleDropInventory(val storage: IItemStorage) extends TileEntityInternalModule[ModuleDropInventory] {
  var shouldDrop: Boolean = true

  override def module: IModule[ModuleDropInventory] = InternalModules.MODULE_DROP_INVENTORY

  override def onBlockBreak(core: ITileEntity, state: IBlockState): Unit = {
    if (shouldDrop) {
      val random = new Random
      storage.foreach(InventoryUtils.dropItem(_, Loc4(core), random))
      storage.indices.foreach(storage.setSlot(_, IItemStack.Empty))
    }
  }
}
