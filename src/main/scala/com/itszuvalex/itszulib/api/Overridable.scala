package com.itszuvalex.itszulib.api

/**
  * Created by Christopher Harris (Itszuvalex) on 3/20/2016.
  */
class Overridable[F](val default: F) {
  private var overrideVal: F = default

  def apply: F = overrideVal

  def revert() = overrideVal = default

  def overrideDefault(defOverride: F): Unit = overrideVal = defOverride
}
