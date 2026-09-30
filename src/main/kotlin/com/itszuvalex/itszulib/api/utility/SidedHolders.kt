package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.api.adapters.ILevel
import net.minecraft.world.level.Level
import net.neoforged.fml.LogicalSide

/**
 * Lazily created value that only exists on one logical side.
 */
class LazySingleSidedHolder<T : Any>(factory: () -> T, private val side: LogicalSide) : ILevelBasedSupplier<T> {
    private val obj by lazy(factory)

    override fun get(level: ILevel): T? = get(SidedHelper.sideFromIsClient(level.isClientSide()))

    override fun get(level: Level): T? = get(SidedHelper.sideFromIsClient(level.isClientSide))

    fun get(side: LogicalSide): T? = if (this.side == side) obj else null
}

/**
 * Calls [supplier] on every query from [side]; returns null on the other side.
 */
class SingleSidedSupplier<T : Any>(private val supplier: () -> T, private val side: LogicalSide) : ILevelBasedSupplier<T> {
    override fun get(level: ILevel): T? = getSided(SidedHelper.sideFromIsClient(level.isClientSide()))

    override fun get(level: Level): T? = getSided(SidedHelper.sideFromIsClient(level.isClientSide))

    private fun getSided(side: LogicalSide): T? = if (this.side == side) supplier() else null
}
