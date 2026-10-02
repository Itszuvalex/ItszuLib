package com.itszuvalex.itszulib.api.utility

/**
 * A value with a default that tests (or other mods) can temporarily override and later [revert].
 */
class Overideable<T : Any>(private val defaultValue: T) {
    private var overrideValue: T? = null

    fun get(): T = overrideValue ?: defaultValue

    fun revert() {
        overrideValue = null
    }

    fun setOverrideValue(overrideValue: T) {
        this.overrideValue = overrideValue
    }

    val isOverriden: Boolean get() = overrideValue != null
}
