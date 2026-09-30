package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.adapters.IModuleProvider
import net.minecraft.core.Direction
import net.neoforged.neoforge.capabilities.BlockCapability

interface IModuleCapabilityMap : IModuleProvider {
    /**
     * @return The capability instance for the given side, or null if not provided. Suitable for returning directly
     * from a NeoForge capability provider.
     */
    fun <T : Any> getCapability(cap: BlockCapability<T, Direction?>, side: Direction?): T?

    fun invalidateFrags()

    fun rehydrateFrags()
}

/**
 * Providers map a nullable side to the object exposed on that side, or null if nothing is exposed there.
 */
interface IMutableModuleCapabilityMap : IModuleCapabilityMap {
    fun <T : Any> addModule(module: IModule<T>, provider: (Direction?) -> T?)

    fun <T : Any> addCapability(cap: BlockCapability<T, Direction?>, provider: (Direction?) -> T?)
}

/**
 * Linear-scan map; cheaper than hashing for the handful of modules a block entity usually exposes.
 */
class ModuleCapabilityArrayListMap : IMutableModuleCapabilityMap {
    private var valid = true

    private class ModulePair<T : Any>(val mod: IModule<T>, val provider: (Direction?) -> T?)
    private class CapPair<T : Any>(val cap: BlockCapability<T, Direction?>, val provider: (Direction?) -> T?)

    private val modList = ArrayList<ModulePair<*>>()
    private val capList = ArrayList<CapPair<*>>()

    override fun <T : Any> addModule(module: IModule<T>, provider: (Direction?) -> T?) {
        modList.add(ModulePair(module, provider))
        module.blockCapability?.let { capList.add(CapPair(it, provider)) }
    }

    override fun <T : Any> addCapability(cap: BlockCapability<T, Direction?>, provider: (Direction?) -> T?) {
        capList.add(CapPair(cap, provider))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getModule(module: IModule<T>, side: Direction?): T? {
        if (!valid) return null
        val pair = modList.firstOrNull { it.mod === module } ?: return null
        return (pair as ModulePair<T>).provider(side)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getCapability(cap: BlockCapability<T, Direction?>, side: Direction?): T? {
        if (!valid) return null
        val pair = capList.firstOrNull { it.cap === cap } ?: return null
        return (pair as CapPair<T>).provider(side)
    }

    override fun invalidateFrags() {
        valid = false
    }

    override fun rehydrateFrags() {
        valid = true
    }
}

class ModuleCapabilityHashMap : IMutableModuleCapabilityMap {
    private var valid = true
    private val modMap = HashMap<IModule<*>, (Direction?) -> Any?>()
    private val capMap = HashMap<BlockCapability<*, Direction?>, (Direction?) -> Any?>()

    override fun <T : Any> addModule(module: IModule<T>, provider: (Direction?) -> T?) {
        modMap[module] = provider
        module.blockCapability?.let { capMap[it] = provider }
    }

    override fun <T : Any> addCapability(cap: BlockCapability<T, Direction?>, provider: (Direction?) -> T?) {
        capMap[cap] = provider
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getModule(module: IModule<T>, side: Direction?): T? {
        if (!valid) return null
        return modMap[module]?.invoke(side) as T?
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getCapability(cap: BlockCapability<T, Direction?>, side: Direction?): T? {
        if (!valid) return null
        return capMap[cap]?.invoke(side) as T?
    }

    override fun invalidateFrags() {
        valid = false
    }

    override fun rehydrateFrags() {
        valid = true
    }
}
