package com.itszuvalex.itszulib.util

/**
  * Created by Christopher Harris (Itszuvalex) on 4/6/15.
  */
object MiscUtils {

  /**
    *
    * @param a [0, 255] alpha value, bit masked to fit
    * @param r [0, 255] alpha value, bit masked to fit
    * @param g [0, 255] alpha value, bit masked to fit
    * @param b [0, 255] alpha value, bit masked to fit
    * @return Helper function for generationg integer representations of argb colors.
    */
  def colorFromARGB(a: Int, r: Int, g: Int, b: Int) = {
    var r1 = 0
    r1 += (a & 255) << 24
    r1 += (r & 255) << 16
    r1 += (g & 255) << 8
    r1 += b & 255
    r1
  }

  /**
    *
    * @param color Color in ARGB format, little endian.
    * @return 4-tuple consisting of a, r, g, b broken down into ints 0-255
    */
  def ARGBFromColor(color: Int): (Int, Int, Int, Int) =
    (
      ((color & (255 << 24)) >> 24) & 255,
      (color & (255 << 16)) >> 16,
      (color & (255 << 8)) >> 8,
      color & 255
      )

}
