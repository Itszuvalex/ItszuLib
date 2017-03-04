package com.itszuvalex.itszulib.api.storage

import net.minecraft.inventory.ISidedInventory
import net.minecraft.util.EnumFacing

/**
  * Created by Christopher Harris (Itszuvalex) on 7/25/16.
  */
class ItemStorageSidedInventory(val inv: ISidedInventory, side: EnumFacing) extends ItemStorageSlice(new ItemStorageInventory(inv), inv.getSlotsForFace(side))
