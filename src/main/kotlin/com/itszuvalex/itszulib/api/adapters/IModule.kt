package com.itszuvalex.itszulib.api.adapters

import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.capabilities.BlockCapability
import net.neoforged.neoforge.capabilities.ItemCapability
import net.neoforged.neoforge.transfer.access.ItemAccess
import org.jetbrains.annotations.TestOnly
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * A named handle for a behaviour, optionally backed by NeoForge capabilities.
 */
interface IModule<T : Any> {
    /**
     * Block capability this module is exposed through, with a nullable [Direction] side context.
     */
    val blockCapability: BlockCapability<T, Direction?>?

    /**
     * Item capability this module is exposed through on item stacks.
     */
    val itemCapability: ItemCapability<T, ItemAccess>?

    /**
     * Unique, namespaced id (e.g. `itszulib:colorable`), so modules from different mods can't collide.
     */
    val id: Identifier
}

interface IModuleProvider {
    fun <T : Any> getModule(module: IModule<T>, side: Direction?): T?
}

class Module<T : Any> private constructor(
    override val id: Identifier,
    override val blockCapability: BlockCapability<T, Direction?>?,
    override val itemCapability: ItemCapability<T, ItemAccess>?,
) : IModule<T> {
    override fun toString(): String = "Module[$id]"

    companion object {
        /**
         * Concurrent because mods may register modules from parallel mod construction.
         */
        private val MODULES = ConcurrentHashMap<Identifier, IModule<*>>()

        /**
         * @throws IllegalArgumentException if a module with this id is already registered.
         */
        @JvmStatic
        @JvmOverloads
        fun <T : Any> registerModule(
            id: Identifier,
            blockCapability: BlockCapability<T, Direction?>?,
            itemCapability: ItemCapability<T, ItemAccess>? = null,
        ): IModule<T> {
            val mod = Module(id, blockCapability, itemCapability)
            require(MODULES.putIfAbsent(id, mod) == null) { "Module with id: $id already registered." }
            return mod
        }

        @JvmStatic
        fun modules(): Collection<IModule<*>> = Collections.unmodifiableCollection(MODULES.values)

        @TestOnly
        @JvmStatic
        fun clear() = MODULES.clear()
    }
}
