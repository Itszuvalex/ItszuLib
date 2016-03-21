package com.itszuvalex.itszulib.api.access

import java.util.concurrent.atomic.AtomicInteger

/**
  * Created by Christopher Harris (Itszuvalex) on 3/12/2016.
  */
trait Revisioned {
  private val revision = new AtomicInteger(0)

  def getRevision: Int = revision.intValue()

  def incrementRevision() = revision.incrementAndGet()
}
