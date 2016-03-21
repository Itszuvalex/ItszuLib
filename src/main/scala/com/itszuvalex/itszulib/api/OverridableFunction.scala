package com.itszuvalex.itszulib.api

/**
  * Created by Christopher Harris (Itszuvalex) on 3/20/2016.
  */
class OverridableFunction[F](val default: F) {
  private var funcOverride = default

  def apply = funcOverride

  def revert() = funcOverride = default

  def overrideFunc(overFunc: F): Unit = funcOverride = overFunc
}
