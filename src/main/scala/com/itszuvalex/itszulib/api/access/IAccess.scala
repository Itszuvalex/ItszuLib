package com.itszuvalex.itszulib.api.access

/**
  * Created by Christopher Harris (Itszuvalex) on 3/26/16.
  */
trait IAccess[A <: IAccess[A, T], T] {
  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing ItemStack
    */
  def get: Option[T]

  /**
    * Sets this item access's storage to the ItemStack.
    *
    * @param stack ItemStack to set this to.
    */
  def set(stack: T): Unit = {onChanged()}

  def isEmpty: Boolean = get.isEmpty

  def isDefined: Boolean = get.isDefined

  def matches(matcher: (Option[T]) => Boolean) = matcher != null && matcher(get)

  def canSetTo(stack: T): Boolean = true

  /**
    * Remove Item and metadata from this access.
    */
  def clear(): Unit

  def onChanged(): Unit

  /**
    * Copies all required info from another A
    *
    * @param other
    */
  def copyFromAccess(other: A, deepCopy: Boolean = true): Unit

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  def isValid: Boolean

}
