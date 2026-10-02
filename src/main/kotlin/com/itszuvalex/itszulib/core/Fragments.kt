package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.utility.IModuleCapabilityMap
import com.itszuvalex.itszulib.api.utility.IMutableModuleCapabilityMap
import com.itszuvalex.itszulib.api.utility.IScopedSerialization
import com.itszuvalex.itszulib.api.utility.ModuleCapabilityArrayListMap
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.neoforge.capabilities.BlockCapability

interface IBlockEntityEventHandler {
    fun invalidateFrags()

    fun rehydrateFrags()
}

interface IBlockEntityBlockEventHandler {
    /**
     * Server side, called when the block is actually replaced (not for block state changes that keep the block
     * entity).
     */
    fun onRemove(level: ILevel, pos: BlockPos, blockStatePrev: BlockState)

    /**
     * Both sides, once the block entity is in a loaded level: after placement, and when its chunk loads (NeoForge's
     * `BlockEntity#onLoad`, which runs once the chunk's block entities are all registered, so neighbours in the same
     * chunk can be looked up).
     */
    fun onLoad(level: ILevel, pos: BlockPos) {}

    /**
     * Both sides, when the block entity's chunk unloads (NeoForge's `BlockEntity#onChunkUnloaded`). Not called when
     * the block is removed (see [onRemove]).
     */
    fun onChunkUnloaded(level: ILevel, pos: BlockPos) {}

    /**
     * Both sides, when a neighbouring block changed (`Block#neighborChanged`; 26.1 no longer says which neighbour).
     * Only blocks extending [EntityBlockCore] forward this.
     */
    fun onNeighborChanged(level: ILevel, pos: BlockPos) {}
}

fun interface IBlockEntityTickable {
    fun tick(level: ILevel, blockPos: BlockPos, blockState: BlockState)
}

/**
 * What a fragment can ask of the block entity that owns it. Handed to each fragment through
 * [IInternalBlockEntityFragment.onAttach] when it is added to a [BlockEntityFragmentCollection].
 */
interface IFragmentHost {
    fun blockEntity(): IBlockEntity

    /**
     * Marks the block entity as changed so it is saved. Call after changing LEVEL-scope state.
     */
    fun markDirty()

    /**
     * [markDirty], and on the server also re-sends DESCRIPTION-scope data to clients tracking the chunk.
     * Call after changing state that clients render or display.
     */
    fun markDirtyAndSync()
}

/**
 * A piece of block entity behaviour with its own lifecycle hooks and serialization, but no exposed module.
 */
interface IInternalBlockEntityFragment : IBlockEntityEventHandler, IBlockEntityBlockEventHandler, IScopedSerialization {
    /**
     * Unique within the block entity; keys this fragment's saved data.
     */
    fun name(): String

    /**
     * Called once when the fragment is added to its block entity's fragment collection.
     */
    fun onAttach(host: IFragmentHost) {}
}

/**
 * A fragment that exposes a module [T].
 */
interface IBlockEntityFragment<T : Any> : IInternalBlockEntityFragment {
    fun module(): IModule<T>

    /**
     * @return Maps a nullable side to the module instance exposed on that side, or null if not exposed there.
     * Called on every query, so it should return the fragment's current instance.
     */
    fun faceToModuleMapper(be: IBlockEntity): (Direction?) -> T?
}

/**
 * The fragments, exposed modules/capabilities and tickables of one block entity.
 */
class BlockEntityFragmentCollection(private val host: IFragmentHost) :
    IBlockEntityEventHandler, IBlockEntityBlockEventHandler, IScopedSerialization, IModuleCapabilityMap, IBlockEntityTickable {
    private val modCapMap: IMutableModuleCapabilityMap = ModuleCapabilityArrayListMap()
    private val modList = ArrayList<IInternalBlockEntityFragment>()
    private val tickList = ArrayList<IBlockEntityTickable>()
    private val fragmentNames = HashSet<String>()
    private val exposedModules = HashSet<IModule<*>>()

    /**
     * @throws IllegalArgumentException if a fragment with the same [IInternalBlockEntityFragment.name] was already
     * added; names key each fragment's saved data, so duplicates would overwrite each other.
     */
    fun addInternalFragment(fragment: IInternalBlockEntityFragment) {
        require(fragmentNames.add(fragment.name())) { "Duplicate fragment name: ${fragment.name()}" }
        modList.add(fragment)
        fragment.onAttach(host)
    }

    /**
     * @throws IllegalArgumentException if the name is a duplicate, or another fragment already exposes this module.
     */
    fun <F : Any> addFragment(fragment: IBlockEntityFragment<F>) {
        require(fragment.module() !in exposedModules) { "Module ${fragment.module().id} is already exposed by another fragment" }
        addInternalFragment(fragment)
        exposedModules.add(fragment.module())
        modCapMap.addModule(fragment.module(), fragment.faceToModuleMapper(host.blockEntity()))
    }

    /**
     * Exposes a capability that is not tied to a module, e.g. one of `ModuleCapabilities.STANDARD`.
     */
    fun <T : Any> addCapability(cap: BlockCapability<T, Direction?>, provider: (Direction?) -> T?) =
        modCapMap.addCapability(cap, provider)

    fun addTickable(tickable: IBlockEntityTickable) {
        tickList.add(tickable)
    }

    override fun tick(level: ILevel, blockPos: BlockPos, blockState: BlockState) =
        tickList.forEach { it.tick(level, blockPos, blockState) }

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) =
        modList.filter { it.handlesScope(scope) }.forEach { it.serializeTo(scope, output.child(it.name())) }

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) =
        modList.filter { it.handlesScope(scope) }.forEach { frag ->
            input.child(frag.name()).ifPresent { frag.deserialize(it, scope) }
        }

    override fun handlesScope(scope: NBTSerializationScope): Boolean = modList.any { it.handlesScope(scope) }

    override fun <T : Any> getModule(module: IModule<T>, side: Direction?): T? = modCapMap.getModule(module, side)

    override fun <T : Any> getCapability(cap: BlockCapability<T, Direction?>, side: Direction?): T? =
        modCapMap.getCapability(cap, side)

    override fun invalidateFrags() {
        modCapMap.invalidateFrags()
        modList.forEach { it.invalidateFrags() }
    }

    override fun rehydrateFrags() {
        modList.forEach { it.rehydrateFrags() }
        modCapMap.rehydrateFrags()
    }

    override fun onRemove(level: ILevel, pos: BlockPos, blockStatePrev: BlockState) =
        modList.forEach { it.onRemove(level, pos, blockStatePrev) }

    override fun onLoad(level: ILevel, pos: BlockPos) = modList.forEach { it.onLoad(level, pos) }

    override fun onChunkUnloaded(level: ILevel, pos: BlockPos) = modList.forEach { it.onChunkUnloaded(level, pos) }

    override fun onNeighborChanged(level: ILevel, pos: BlockPos) = modList.forEach { it.onNeighborChanged(level, pos) }
}
