package com.itszuvalex.itszulib.util

/**
 * Immutable ARGB color. Use the `with*` methods to derive a changed copy.
 */
data class Color(val alpha: Byte, val red: Byte, val green: Byte, val blue: Byte) {
    /**
     * @param argb Packed as `0xAARRGGBB`.
     */
    constructor(argb: Int) : this((argb ushr 24).toByte(), (argb ushr 16).toByte(), (argb ushr 8).toByte(), argb.toByte())

    /**
     * @return Packed as `0xAARRGGBB`.
     */
    fun toInt(): Int =
        (alpha.toInt() and 255 shl 24) or (red.toInt() and 255 shl 16) or (green.toInt() and 255 shl 8) or (blue.toInt() and 255)

    fun withAlpha(alpha: Byte) = copy(alpha = alpha)

    fun withRed(red: Byte) = copy(red = red)

    fun withGreen(green: Byte) = copy(green = green)

    fun withBlue(blue: Byte) = copy(blue = blue)

    companion object {
        @JvmField
        val TRANSPARENT = Color(0)
    }
}
