package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.api.adapters.ILevel
import net.minecraft.world.level.Level

/**
 * Gates in-memory data against an [ILevel] or [Level]. This masks how the data is stored: capabilities, an
 * in-memory map, or sided memory that uses [Level.isClientSide] to return the appropriate storage.
 *
 * @param T Data holder
 */
interface ILevelBasedSupplier<T : Any> {
    fun get(level: ILevel): T?

    fun get(level: Level): T?
}
