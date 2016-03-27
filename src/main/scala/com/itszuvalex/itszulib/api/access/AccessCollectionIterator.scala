package com.itszuvalex.itszulib.api.access

/**
  * Created by Christopher Harris (Itszuvalex) on 3/12/2016.
  */
class AccessCollectionIterator[C <: ICollectionAccess[C, A], A <: IAccess[A, _]](private[access] val collection: C) extends Iterator[A] {
  private[access] val revision = collection.getRevision
  private[access] var index    = 0

  override def hasNext: Boolean = isValid && index < collection.length

  def isValid: Boolean = revision == collection.getRevision

  override def next(): A = {
    val ret = collection(index)
    index += 1
    ret
  }
}
