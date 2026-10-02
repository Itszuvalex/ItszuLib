package com.itszuvalex.itszulib.core.frag

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.multiblock.IMultiblockMember
import com.itszuvalex.itszulib.api.multiblock.IMultiblockState
import com.itszuvalex.itszulib.api.multiblock.MultiblockInstance
import com.itszuvalex.itszulib.api.multiblock.MultiblockManager
import com.itszuvalex.itszulib.api.multiblock.MultiblockMembership
import com.itszuvalex.itszulib.api.multiblock.MultiblockRoleRef
import com.itszuvalex.itszulib.api.multiblock.MultiblockShape
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.core.IBlockEntityTickable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * Fragment-based [IMultiblockMember], with shared state and client sync.
 *
 * Persists [membership] (LEVEL scope, and DESCRIPTION scope so clients know whether the block is formed). As the home
 * member of a stateful shape it also creates the shape's state on [join] and saves it (LEVEL scope, under
 * [STATE_KEY]), so the state is saved with the home member's chunk. Exposed through [Modules.MULTIBLOCK_MEMBER].
 *
 * @param autoForm See [IMultiblockMember.autoForm].
 * @param manager The server's manager; replaceable for tests.
 */
class FragMultiblockPart @JvmOverloads constructor(
    override val candidateRoles: List<MultiblockRoleRef>,
    override val autoForm: Boolean = true,
    private val manager: () -> MultiblockManager = { MultiblockManager.SERVER },
) : BlockEntityFragment<IMultiblockMember>(), IMultiblockMember {
    override var membership: MultiblockMembership? = null
        private set

    override var state: IMultiblockState? = null
        private set

    private var isClient = false

    /**
     * Client side there is no manager: each part keeps a scratch state of its shape for menus to sync into.
     */
    private var clientState: IMultiblockState? = null

    val isFormed: Boolean get() = membership != null

    val isHome: Boolean get() = membership?.isHome == true

    /**
     * The structure's shared state: this member's own as home, otherwise the home member's if it is loaded. Client
     * side, a scratch state of the shape (see [clientState]); null while not formed.
     */
    fun sharedState(): IMultiblockState? {
        val m = membership ?: return null
        if (isClient) return clientState ?: m.shape.stateFactory?.invoke(Runnable {})?.also { clientState = it }
        return if (m.isHome) state else manager().stateOf(this)
    }

    /**
     * This member's loaded structure (server side).
     */
    fun instance(): MultiblockInstance? = if (isClient) null else manager().instanceOf(this)

    override fun join(membership: MultiblockMembership) {
        this.membership = membership
        state = if (membership.isHome) membership.shape.stateFactory?.invoke(Runnable { markDirty() }) else null
        markDirtyAndSync()
    }

    override fun leave() {
        membership = null
        state = null
        clientState = null
        markDirtyAndSync()
    }

    override fun module(): IModule<IMultiblockMember> = Modules.MULTIBLOCK_MEMBER

    override fun faceToModuleMapper(be: IBlockEntity): (Direction?) -> IMultiblockMember? = { this }

    override fun name(): String = NAME

    override fun handlesScope(scope: NBTSerializationScope): Boolean = scope != NBTSerializationScope.ITEM

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) {
        val m = membership ?: return
        output.store(ID_TAG, UUIDUtil.CODEC, m.structureId)
        output.putString(SHAPE_TAG, m.shape.id.toString())
        output.store(OFFSET_TAG, BlockPos.CODEC, m.offset)
        if (scope == NBTSerializationScope.LEVEL) state?.serialize(output.child(STATE_KEY))
    }

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) {
        val shape = input.getString(SHAPE_TAG).map(Identifier::tryParse).map { it?.let(MultiblockShape::byId) }.orElse(null)
        val id = input.read(ID_TAG, UUIDUtil.CODEC).orElse(null)
        val loaded = if (shape == null || id == null) null else MultiblockMembership(id, shape, input.read(OFFSET_TAG, BlockPos.CODEC).orElse(BlockPos.ZERO))
        if (loaded?.structureId != membership?.structureId) clientState = null
        membership = loaded
        if (scope != NBTSerializationScope.LEVEL) return
        state = if (loaded != null && loaded.isHome) loaded.shape.stateFactory?.invoke(Runnable { markDirty() }) else null
        state?.let { s -> input.child(STATE_KEY).ifPresent(s::deserialize) }
    }

    override fun onLoad(level: ILevel, pos: BlockPos) {
        isClient = level.isClientSide()
        if (isClient) return
        manager().onPartLoaded(level, pos, this)
    }

    override fun onChunkUnloaded(level: ILevel, pos: BlockPos) {
        if (level.isClientSide()) return
        manager().onPartUnloaded(this)
    }

    override fun onRemove(level: ILevel, pos: BlockPos, blockStatePrev: BlockState) {
        if (level.isClientSide()) return
        manager().onPartRemoved(level, pos, this)
    }

    companion object {
        const val NAME = "MultiblockPart"
        const val ID_TAG = "id"
        const val SHAPE_TAG = "shape"
        const val OFFSET_TAG = "offset"
        const val STATE_KEY = "state"
    }
}

/**
 * Ticks a formed structure once per server tick, from whichever of its members ticks first, so it runs while any
 * member is in a ticking chunk. Add with [com.itszuvalex.itszulib.core.BlockEntityFragmentCollection.addTickable] (or
 * [addTickableFragment]).
 *
 * @param gameTime The level's game time; replaceable for tests.
 */
abstract class FragMultiblockTickable @JvmOverloads constructor(
    protected val part: FragMultiblockPart,
    private val gameTime: (ILevel) -> Long = { it.toMinecraft().gameTime },
) : InternalBlockEntityFragment(), IBlockEntityTickable {
    override fun tick(level: ILevel, blockPos: BlockPos, blockState: BlockState) {
        if (level.isClientSide()) return
        val instance = part.instance() ?: return
        if (!instance.claimTick(gameTime(level))) return
        serverStructureTick(level, instance)
    }

    /**
     * Runs once per server tick for the structure; [MultiblockInstance.anchorPos] is the home member's position and
     * [MultiblockInstance.state] the shared state (null while the home chunk is loading).
     */
    abstract fun serverStructureTick(level: ILevel, instance: MultiblockInstance)
}
