package com.itszuvalex.itszulib.core.frag

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.storage.IFluidStorage
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.api.wrappers.WrapperResourceHandlerIFluidStorage
import com.itszuvalex.itszulib.api.wrappers.WrapperResourceHandlerIItemStorage
import com.itszuvalex.itszulib.core.BlockEntityFragmentCollection
import com.itszuvalex.itszulib.core.EnumAutomaticIO
import com.itszuvalex.itszulib.core.IBlockEntityTickable
import com.itszuvalex.itszulib.core.SidedStorageConfiguration
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.neoforge.capabilities.BlockCapability
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.ResourceHandlerUtil
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.item.ItemResource
import net.neoforged.neoforge.transfer.resource.Resource
import java.util.IdentityHashMap
import net.neoforged.neoforge.capabilities.Capabilities as NeoCapabilities

/**
 * Exposes a storage as a module, per side, and as the matching NeoForge capability (see [addItemStorage] /
 * [addFluidStorage]). Port of ItszuLib 1.12.2's `ModuleIItemStorage`/`ModuleIFluidStorage` plus their
 * `Module*HandlerConverter`s.
 *
 * Per side: with no side, [storage]; otherwise the storage the block entity's sided configuration (module
 * [configModule]) assigns to that face, or [storage] when there is no configuration. A face the configuration maps to
 * no storage exposes nothing.
 *
 * @param persist Whether to save [storage] (LEVEL scope). False for views of storage owned elsewhere, e.g. a multiblock
 * part forwarding to its controller.
 */
abstract class FragStorage<S : Any, R : Resource>(
    private val name: String,
    val storage: S,
    private val configModule: IModule<out SidedStorageConfiguration<S>>,
    private val persist: Boolean,
) : BlockEntityFragment<S>() {
    private val handlers = IdentityHashMap<S, ResourceHandler<R>>()

    /**
     * @return The storage exposed on [side], or null if that face exposes none.
     */
    fun storageFor(side: Direction?): S? {
        if (side == null) return storage
        val config = host?.blockEntity()?.getModule(configModule, null) ?: return storage
        return config.getStorageForGlobalFacing(side)
    }

    /**
     * @return The NeoForge handler over [storageFor] ([side]), one cached instance per storage, so transactions on the
     * same storage share snapshot journals.
     */
    fun handler(side: Direction?): ResourceHandler<R>? = storageFor(side)?.let { s -> handlers.getOrPut(s) { wrap(s) } }

    protected abstract fun wrap(storage: S): ResourceHandler<R>

    protected abstract fun serializeStorage(output: ValueOutput)

    protected abstract fun deserializeStorage(input: ValueInput)

    override fun name(): String = name

    override fun faceToModuleMapper(be: IBlockEntity): (Direction?) -> S? = ::storageFor

    override fun handlesScope(scope: NBTSerializationScope): Boolean = persist && scope == NBTSerializationScope.LEVEL

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) = serializeStorage(output)

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) = deserializeStorage(input)
}

class FragItemStorage @JvmOverloads constructor(storage: IItemStorage, name: String = NAME, persist: Boolean = true) :
    FragStorage<IItemStorage, ItemResource>(name, storage, Modules.ITEM_STORAGE_CONFIGURABLE, persist) {
    override fun module(): IModule<IItemStorage> = Modules.ITEM_STORAGE

    override fun wrap(storage: IItemStorage): ResourceHandler<ItemResource> = WrapperResourceHandlerIItemStorage.of(storage)

    override fun serializeStorage(output: ValueOutput) = storage.serialize(output)

    override fun deserializeStorage(input: ValueInput) = storage.deserialize(input)

    companion object {
        const val NAME = "ItemStorage"
    }
}

class FragFluidStorage @JvmOverloads constructor(storage: IFluidStorage, name: String = NAME, persist: Boolean = true) :
    FragStorage<IFluidStorage, FluidResource>(name, storage, Modules.FLUID_STORAGE_CONFIGURABLE, persist) {
    override fun module(): IModule<IFluidStorage> = Modules.FLUID_STORAGE

    override fun wrap(storage: IFluidStorage): ResourceHandler<FluidResource> = WrapperResourceHandlerIFluidStorage.of(storage)

    override fun serializeStorage(output: ValueOutput) = storage.serialize(output)

    override fun deserializeStorage(input: ValueInput) = storage.deserialize(input)

    companion object {
        const val NAME = "FluidStorage"
    }
}

/**
 * Adds [frag] and exposes it through NeoForge's item capability.
 */
fun BlockEntityFragmentCollection.addItemStorage(frag: FragItemStorage): FragItemStorage {
    addFragment(frag)
    addCapability(NeoCapabilities.Item.BLOCK, frag::handler)
    return frag
}

/**
 * Adds [frag] and exposes it through NeoForge's fluid capability.
 */
fun BlockEntityFragmentCollection.addFluidStorage(frag: FragFluidStorage): FragFluidStorage {
    addFragment(frag)
    addCapability(NeoCapabilities.Fluid.BLOCK, frag::handler)
    return frag
}

/**
 * Adds a fragment that also ticks.
 */
fun <F> BlockEntityFragmentCollection.addTickableFragment(frag: F): F where F : IBlockEntityTickable, F : com.itszuvalex.itszulib.core.IInternalBlockEntityFragment {
    addInternalFragment(frag)
    addTickable(frag)
    return frag
}

/**
 * Exposes a sided storage configuration as [module], saved and synced to clients (so screens can show it). Port of
 * ItszuLib 1.12.2's `ModuleIItemSidedConfiguration`/`ModuleIFluidSidedConfiguration`.
 *
 * Change the configuration through [update], so the change is saved and synced.
 */
class FragSidedConfiguration<C : SidedStorageConfiguration<*>>(
    private val name: String,
    val configuration: C,
    private val module: IModule<C>,
) : BlockEntityFragment<C>() {
    fun update(change: (C) -> Unit) {
        change(configuration)
        markDirtyAndSync()
    }

    override fun name(): String = name

    override fun module(): IModule<C> = module

    override fun faceToModuleMapper(be: IBlockEntity): (Direction?) -> C? = { configuration }

    override fun handlesScope(scope: NBTSerializationScope): Boolean = scope != NBTSerializationScope.ITEM

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) = configuration.serialize(output)

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) = configuration.deserialize(input)
}

/**
 * Every [ticksPerOperation] ticks, pulls from the neighbours on INPUT faces into the storage the sided configuration
 * assigns to that face, and pushes from OUTPUT faces' storages into the neighbours, moving up to [amountPerOperation]
 * in each direction in total. Neighbours are reached through NeoForge's item or fluid capability. Port of ItszuLib
 * 1.12.2's `ModuleIItemAutoIO`/`ModuleIFluidAutoIO` and `TileEntityUtils.checkDo*IO`.
 */
abstract class FragAutoIO<S : Any, R : Resource>(
    private val name: String,
    private val configModule: IModule<out SidedStorageConfiguration<S>>,
    private val capability: BlockCapability<ResourceHandler<R>, Direction?>,
    private val ticksPerOperation: () -> Int,
    private val amountPerOperation: () -> Int,
) : InternalBlockEntityFragment(), IBlockEntityTickable {
    /**
     * Ticks until the next operation; the operation runs when this reaches 0.
     */
    var ticks = 0
        private set

    private val handlers = IdentityHashMap<S, ResourceHandler<R>>()

    protected abstract fun wrap(storage: S): ResourceHandler<R>

    /**
     * Moves up to [amount] from [from] to [to] in a root transaction.
     *
     * @return The amount moved.
     */
    protected open fun move(from: ResourceHandler<R>, to: ResourceHandler<R>, amount: Int): Int =
        ResourceHandlerUtil.move(from, to, { true }, amount, null)

    override fun tick(level: ILevel, blockPos: BlockPos, blockState: BlockState) {
        if (level.isClientSide()) return
        ticks = incrementTicks(ticks, ticksPerOperation())
        if (ticks != 0) return
        val config = host?.blockEntity()?.getModule(configModule, null) ?: return
        val amount = amountPerOperation()
        var input = amount
        var output = amount
        for (face in Direction.entries) {
            val io = config.getIOForAbsoluteFacing(face)
            if (io == EnumAutomaticIO.NONE) continue
            if (io == EnumAutomaticIO.INPUT && input <= 0 || io == EnumAutomaticIO.OUTPUT && output <= 0) continue
            val ours = config.getStorageForGlobalFacing(face)?.let(::handler) ?: continue
            val neighbour = neighbour(level, blockPos.relative(face), face.opposite) ?: continue
            if (io == EnumAutomaticIO.INPUT) input -= move(neighbour, ours, input)
            else output -= move(ours, neighbour, output)
        }
        if (input != amount || output != amount) markDirty()
    }

    /**
     * @return The NeoForge handler over [storage], one cached instance per storage (as [FragStorage.handler] does).
     */
    fun handler(storage: S): ResourceHandler<R> = handlers.getOrPut(storage) { wrap(storage) }

    /**
     * @return The neighbour's handler on [side], or null. Never loads a chunk.
     */
    protected open fun neighbour(level: ILevel, pos: BlockPos, side: Direction): ResourceHandler<R>? {
        if (!level.isLoaded(pos)) return null
        return level.toMinecraft().getCapability(capability, pos, side)
    }

    override fun name(): String = name

    override fun handlesScope(scope: NBTSerializationScope): Boolean = scope == NBTSerializationScope.LEVEL

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) = output.putInt(TICKS_KEY, ticks)

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) {
        ticks = input.getIntOr(TICKS_KEY, 0)
    }

    companion object {
        const val TICKS_KEY = "Ticks"

        /**
         * Counts [ticks] down, wrapping to `ticksToAct - 1` after 0; always 0 if [ticksToAct] is not positive.
         */
        @JvmStatic
        fun incrementTicks(ticks: Int, ticksToAct: Int): Int = if (ticksToAct <= 0) 0 else Math.floorMod(ticks - 1, ticksToAct)
    }
}

class FragItemAutoIO @JvmOverloads constructor(
    ticksPerOperation: () -> Int = { TICKS_DEFAULT },
    amountPerOperation: () -> Int = { AMOUNT_DEFAULT },
    name: String = NAME,
) : FragAutoIO<IItemStorage, ItemResource>(
    name, Modules.ITEM_STORAGE_CONFIGURABLE, NeoCapabilities.Item.BLOCK, ticksPerOperation, amountPerOperation,
) {
    override fun wrap(storage: IItemStorage): ResourceHandler<ItemResource> = WrapperResourceHandlerIItemStorage.of(storage)

    /**
     * Stacks onto matching items before filling empty slots, as 1.12.2's `transferIntoStorage` did.
     */
    override fun move(from: ResourceHandler<ItemResource>, to: ResourceHandler<ItemResource>, amount: Int): Int =
        ResourceHandlerUtil.moveStacking(from, to, { true }, amount, null)

    companion object {
        const val NAME = "ItemAutoIO"
        const val TICKS_DEFAULT = 20
        const val AMOUNT_DEFAULT = 1
    }
}

class FragFluidAutoIO @JvmOverloads constructor(
    ticksPerOperation: () -> Int = { TICKS_DEFAULT },
    amountPerOperation: () -> Int = { AMOUNT_DEFAULT },
    name: String = NAME,
) : FragAutoIO<IFluidStorage, FluidResource>(
    name, Modules.FLUID_STORAGE_CONFIGURABLE, NeoCapabilities.Fluid.BLOCK, ticksPerOperation, amountPerOperation,
) {
    override fun wrap(storage: IFluidStorage): ResourceHandler<FluidResource> = WrapperResourceHandlerIFluidStorage.of(storage)

    companion object {
        const val NAME = "FluidAutoIO"
        const val TICKS_DEFAULT = 20
        const val AMOUNT_DEFAULT = 250
    }
}
