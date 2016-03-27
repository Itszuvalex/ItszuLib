package com.itszuvalex.itszulib.api.storage


/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/16.
  */
class StorageIterator[S <: IStorage[S, _, _, D], D](private val storage: S) extends Iterator[D] {
  private var index    = 0
  private val revision = storage.getRevision

  override def hasNext: Boolean = isValid && index < storage.length

  def isValid: Boolean = revision == storage.getRevision

  override def next(): D = {
    val ret = storage(index)
    index += 1
    ret
  }
}
