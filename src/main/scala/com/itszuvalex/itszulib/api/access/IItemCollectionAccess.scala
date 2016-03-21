package com.itszuvalex.itszulib.api.access

import net.minecraft.entity.player.EntityPlayer

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/16.
  */

trait IItemCollectionAccess extends scala.collection.immutable.Seq[IItemAccess] with Revisioned {
  def hasEmptySpace: Boolean = exists(_.isEmpty)

  def hasItems: Boolean = exists(_.isDefined)

  def canPlayerAccess(player: EntityPlayer): Boolean

  /**
    * Called when an item in the inventory changes.
    *
    * @param index Index of changed item, or -1 if unknown/multiple.
    */
  def onInventoryChanged(index: Int): Unit = {}

  def copyFromAccess(access: IItemCollectionAccess, copy: Boolean = true) = {
    if (length == access.length) {
      (this zip access).foreach(pair => pair._1.copyFromAccess(pair._2, copy))
      incrementRevision()
    }
  }

  override def iterator: Iterator[IItemAccess] = new AccessCollectionIterator[IItemCollectionAccess, IItemAccess](this)
}

