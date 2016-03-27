package com.itszuvalex.itszulib.api.access

import net.minecraft.entity.player.EntityPlayer

/**
  * Created by Christopher Harris (Itszuvalex) on 3/26/16.
  */
trait ICollectionAccess[C <: ICollectionAccess[C, A], A <: IAccess[A, _]] extends scala.collection.immutable.Seq[A] with Revisioned {
  def hasEmptySpace: Boolean = exists(_.isEmpty)

  def hasFilledSpace: Boolean = exists(_.isDefined)

  def canPlayerAccess(player: EntityPlayer): Boolean

  /**
    * Called when an item in the inventory changes.
    *
    * @param index Index of changed item, or -1 if unknown/multiple.
    */
  def onChanged(index: Int): Unit = {}

  def copyFromAccess(access: C, copy: Boolean = true) = {
    if (length == access.length) {
      (this zip access).foreach(pair => pair._1.copyFromAccess(pair._2, copy))
      incrementRevision()
    }
  }

  override def iterator: Iterator[A] = new AccessCollectionIterator[C, A](this.asInstanceOf[C])
}
