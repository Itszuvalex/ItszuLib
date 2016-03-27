package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IAccess, ICollectionAccess, Revisioned}
import com.itszuvalex.itszulib.api.core.NBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 3/26/16.
  */
trait IStorage[T <: IStorage[T, C, A, D], C <: ICollectionAccess[C, A], A <: IAccess[A, D], D] extends scala.collection.mutable.Seq[D] with NBTSerializable with Revisioned {

  def getFullAccess: C

  def getSingleAccess(slot: Int): A = getFullAccess(slot)

  /**
    *
    * @param size  New size to set to.  This will trim elements if size is smaller than current size.
    *              Otherwise, it will pad default elements. (Nulls)
    * @param clear Set to true to null out the array before the resize.  This is independent of trimming.
    * @return
    */
  def setSize(size: Int, clear: Boolean): Boolean

  override def iterator: Iterator[D] = new StorageIterator[T, D](this.asInstanceOf[T])
}
