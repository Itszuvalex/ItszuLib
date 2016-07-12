package com.itszuvalex.itszulib.api.access

import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.items.IItemHandlerModifiable

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */
class ItemHandlerItemCollectionAccess(private[access] var handler: IItemHandlerModifiable) extends IItemCollectionAccess {
  override def canPlayerAccess(player: EntityPlayer): Boolean = true

  override def length: Int = handler.getSlots

  override def apply(idx: Int): IItemAccess = new ItemHandlerItemAccess(this, idx)

  private[itszuvalex] def updateBackingStore(inv: IItemHandlerModifiable) = {
    handler = inv
    onChanged(-1)
  }
}
