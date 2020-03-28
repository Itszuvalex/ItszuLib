package com.itszuvalex.itszulib.api

/**
  * Created by Christopher Harris (Itszuvalex) on 3/20/2016.
  */
class Overridable[F](val default: F) {
  private var overrideVal: Option[F] = None

  def apply: F = overrideVal.getOrElse(default)

  def revert(): Unit = overrideVal = None

  def overrideDefault(defOverride: F): Unit = overrideVal = Option(defOverride)

  def isOverridden: Boolean = overrideVal.isDefined
}
