package com.itszuvalex.itszulib.api.core

import scala.xml.Node

/**
  * Created by Christopher Harris (Itszuvalex) on 3/29/15.
  */
trait XMLSerializable {

  def saveAsNode: Node

  def loadFromNode(node: Node): Unit
}
