package com.itszuvalex.itszulib.api.adapters

import com.itszuvalex.itszulib.util.Color

/**
 * Something with a color that can be read and changed, e.g. through the
 * [com.itszuvalex.itszulib.api.Capabilities.COLORABLE] capability.
 */
interface IColorable {
    fun getColor(): Color

    /**
     * Implementations persist and sync the change as needed.
     */
    fun setColor(color: Color)
}
