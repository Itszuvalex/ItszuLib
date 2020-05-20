package com.itszuvalex.itszulib.api.storage

trait IStack[T] {

  import scala.math.Numeric.Implicits._

  def amount: T

  def amount_=(amount: T): Unit

  def amountMax: T

  def fill(amt: T)(implicit n: Numeric[T]): T = {
    val toFill = n.min(amt, amountMax - amount)
    amount += toFill
    toFill
  }

  def drain(amt: T)(implicit n: Numeric[T]): T = {
    val toDrain = n.min(amt, amount)
    amount -= toDrain
    toDrain
  }
}

