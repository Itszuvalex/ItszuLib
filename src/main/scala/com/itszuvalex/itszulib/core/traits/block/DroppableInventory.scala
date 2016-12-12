package com.itszuvalex.itszulib.core.traits.block

import java.util.Random

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.Converter
import com.itszuvalex.itszulib.util.InventoryUtils
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.inventory.IInventory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
  * Created by Itszuvalex on 11/28/14.
  */
trait DroppableInventory extends Block {
  var shouldDrop = true

  override def breakBlock(worldIn: World, pos: BlockPos, state: IBlockState): Unit = {
    def dropAllInInv(i: IItemStorage) = {
      val random = new Random
      i.foreach(InventoryUtils.dropItem(_, new Loc4(worldIn, pos), random))
    }

    if (shouldDrop) {
      worldIn.getTileEntity(pos) match {
        case ti: IItemStorage =>
          dropAllInInv(ti)
        case ti: IInventory =>
          dropAllInInv(Converter.IItemStorageFromIInventory(ti))
        case _ =>
      }
    }
    super.breakBlock(worldIn, pos, state)
  }
}
