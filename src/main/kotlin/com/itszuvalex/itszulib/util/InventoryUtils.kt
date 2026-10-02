package com.itszuvalex.itszulib.util

import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.storage.IItemStorage
import net.minecraft.core.BlockPos
import net.minecraft.world.Containers

/**
 * Lazily created instance whose factory can be swapped (e.g. by tests).
 */
class Singleton<T : Any>(private var instanceSupplier: () -> T) {
    private var instance: T? = null

    fun get(): T = instance ?: instanceSupplier().also { instance = it }

    fun reset() {
        instance = null
    }

    fun setSupplier(supplier: () -> T) {
        instanceSupplier = supplier
        reset()
    }
}

interface IInventoryUtils {
    fun dropStorage(level: ILevel, pos: BlockPos, storage: IItemStorage) {
        for (i in 0 until storage.size()) dropItem(level, pos, storage.get(i))
    }

    fun dropItem(level: ILevel, pos: BlockPos, item: IItemStack)

    companion object {
        @JvmField
        val instance: Singleton<IInventoryUtils> = Singleton(::InventoryUtils)
    }
}

class InventoryUtils : IInventoryUtils {
    override fun dropItem(level: ILevel, pos: BlockPos, item: IItemStack) {
        if (item.isEmpty()) return
        Containers.dropItemStack(level.toMinecraft(), pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), item.toMinecraft())
    }
}
