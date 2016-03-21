package com.itszuvalex.itszulib.util

/**
  * Created by Christopher Harris (Itszuvalex) on 7/3/15.
  */
case class Color(var alpha: Byte, var red: Byte, var green: Byte, var blue: Byte) {
  def this(color: Int) =
    this((((color & (255 << 24)) >> 24) & 255).toByte,
         (((color & (255 << 16)) >> 16) & 255).toByte,
         (((color & (255 << 8)) >> 8) & 255).toByte,
         ((color & 255) & 255).toByte)

  def toInt: Int = {
    var r1 = 0
    r1 += (alpha & 255) << 24
    r1 += (red & 255) << 16
    r1 += (green & 255) << 8
    r1 += blue & 255
    r1
  }

  def setRed(red: Byte) = {
    this.red = red
    this
  }

  def setGreen(green: Byte) = {
    this.green = green
    this
  }

  def setBlue(green: Byte) = {
    this.green = green
    this
  }

  def setAlpha(alpha: Byte) = {
    this.alpha = alpha
    this
  }
}
