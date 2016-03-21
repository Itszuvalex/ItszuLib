package com.itszuvalex.itszulib.api.access

import net.minecraft.entity.player.EntityPlayer

/**
  * Created by Christopher Harris (Itszuvalex) on 3/11/2016.
  */
class ItemAccessCollectionAccess(private[access] val sequence: Seq[IItemAccess]) extends IItemCollectionAccess {
  override def canPlayerAccess(player: EntityPlayer): Boolean = true

  override def length: Int = sequence.length

  override def apply(idx: Int): IItemAccess = sequence(idx)
}
