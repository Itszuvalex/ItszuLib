package com.itszuvalex.itszulib.initialization

trait IInitializable {
  def preInit(): Unit = {}

  def init(): Unit = {}

  def postInit(): Unit = {}
}
