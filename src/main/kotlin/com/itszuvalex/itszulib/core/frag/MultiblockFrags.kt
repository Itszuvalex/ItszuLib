package com.itszuvalex.itszulib.core.frag

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.multiblock.MultiBlockInfo
import com.itszuvalex.itszulib.api.multiblock.MultiblockUtils
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.core.IBlockEntityTickable
import com.itszuvalex.itszulib.core.IFragmentHost
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.neoforge.common.util.ValueIOSerializable

/**
 * Exposes [info] through [Modules.MULTIBLOCK], saved and synced to clients; forming or breaking saves and syncs. Port
 * of ItszuLib 1.12.2's `ModuleMultiblockInfo`.
 *
 * Add it before fragments that read [info] while loading (e.g. [FragMultiblockState]): fragments load in the order
 * they were added.
 */
class FragMultiBlockInfo @JvmOverloads constructor(val info: MultiBlockInfo = MultiBlockInfo()) : BlockEntityFragment<MultiBlockInfo>() {
    override fun onAttach(host: IFragmentHost) {
        super.onAttach(host)
        info.onChanged = Runnable { markDirtyAndSync() }
    }

    /**
     * @return The block entity of this multiblock's controller, if formed and loaded.
     */
    fun controller(): IBlockEntity? {
        val be = host?.blockEntity() ?: return null
        val level = levelOf(be) ?: return null
        return MultiblockUtils.controller(level, info)
    }

    /**
     * Finds the level of the owning block entity. A seam for unit tests, which have no vanilla level.
     */
    @JvmField
    var levelOf: (IBlockEntity) -> ILevel? = { be -> be.toMinecraft().level?.let(ILevel::of) }

    /**
     * @return [module] as exposed by this multiblock's controller (side-less), if formed and loaded.
     */
    fun <T : Any> controllerModule(module: IModule<T>): T? = controller()?.getModule(module, null)

    override fun name(): String = NAME

    override fun module(): IModule<MultiBlockInfo> = Modules.MULTIBLOCK

    override fun faceToModuleMapper(be: IBlockEntity): (Direction?) -> MultiBlockInfo? = { info }

    override fun handlesScope(scope: NBTSerializationScope): Boolean = scope != NBTSerializationScope.ITEM

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) = info.serialize(output)

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) = info.deserialize(input)

    companion object {
        const val NAME = "MultiBlockInfo"
    }
}

/**
 * State shared by a whole multiblock, held by its controller. Port of ItszuLib 1.12.2's `MultiblockStateHolder`.
 * [get] on any part returns the controller's state (created on first use); only the controller saves it (LEVEL
 * scope, under [STATE_KEY]).
 *
 * @param holderOf Finds this fragment's counterpart on another block entity of the same multiblock (the controller).
 */
class FragMultiblockState<S : ValueIOSerializable>(
    private val info: FragMultiBlockInfo,
    private val factory: () -> S,
    private val holderOf: (IBlockEntity) -> FragMultiblockState<S>?,
    private val name: String = NAME,
) : InternalBlockEntityFragment() {
    private var state: S? = null

    /**
     * @return The multiblock's state, or null if not formed or the controller is not loaded.
     */
    fun get(): S? {
        if (!isFormedController()) dropStale()
        if (!info.info.isFormed) return null
        if (info.info.isController) return local()
        return info.controller()?.let(holderOf)?.controllerLocal()
    }

    fun hasState(): Boolean {
        if (!isFormedController()) dropStale()
        return state != null
    }

    /**
     * Runs [action] on the state if this block is the controller.
     *
     * @return True if it ran.
     */
    fun doIfController(action: (S) -> Unit): Boolean {
        if (!isFormedController()) {
            dropStale()
            return false
        }
        action(local())
        return true
    }

    private fun isFormedController(): Boolean = info.info.isFormed && info.info.isController

    /**
     * A block that is no longer a formed multiblock's controller forgets the state it held. Only a controller saves
     * its state, so keeping it would make a re-formed multiblock's state depend on whether the chunk was reloaded in
     * between (1.12.2 kept it in memory until then).
     */
    private fun dropStale() {
        state = null
    }

    private fun controllerLocal(): S? = if (isFormedController()) local() else null

    private fun local(): S = state ?: factory().also { state = it }

    /**
     * Drops the held state, e.g. when the multiblock breaks.
     */
    fun clear() {
        state = null
        markDirty()
    }

    override fun name(): String = name

    override fun handlesScope(scope: NBTSerializationScope): Boolean = scope == NBTSerializationScope.LEVEL

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) {
        if (info.info.isController) state?.serialize(output.child(STATE_KEY))
    }

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) {
        if (!info.info.isController) return
        input.child(STATE_KEY).ifPresent { local().deserialize(it) }
    }

    companion object {
        const val NAME = "MultiblockState"
        const val STATE_KEY = "MultiblockState"
    }
}

/**
 * Ticks only on a formed multiblock's controller. Port of ItszuLib 1.12.2's `TileEntityMultiblockTickableModule`.
 * Add with [com.itszuvalex.itszulib.core.BlockEntityFragmentCollection.addTickable] (or [addTickableFragment]).
 */
abstract class FragMultiblockTickable(protected val info: MultiBlockInfo) : InternalBlockEntityFragment(), IBlockEntityTickable {
    override fun tick(level: ILevel, blockPos: BlockPos, blockState: BlockState) {
        if (!info.isFormed || !info.isController) return
        if (level.isClientSide()) clientControllerTick(level, blockPos) else serverControllerTick(level, blockPos)
    }

    open fun clientControllerTick(level: ILevel, pos: BlockPos) {}

    open fun serverControllerTick(level: ILevel, pos: BlockPos) {}
}
