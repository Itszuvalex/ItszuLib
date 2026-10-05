package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.Components
import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.utility.IScopedSerialization
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.api.utility.SidedHelper
import com.mojang.logging.LogUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponentGetter
import net.minecraft.core.component.DataComponentMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.util.ProblemReporter
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.redstone.Orientation
import net.minecraft.world.phys.BlockHitResult
import net.neoforged.fml.LogicalSide
import net.neoforged.neoforge.capabilities.BlockCapability

/**
 * Block entity whose behaviour is composed of fragments (see [BlockEntityFragmentCollection]), added in the
 * subclass constructor through [fragList].
 *
 * Lifecycle wiring: world save/load uses the LEVEL scope, client sync (chunk load, block update packets) the
 * DESCRIPTION scope, the block's item form the ITEM scope (via [Components.FRAGMENT_DATA]). Each fragment writes into
 * its own child, keyed by its name, under [FRAG_KEY].
 */
open class BlockEntityCore(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    BlockEntity(type, pos, state), IBlockEntity, IBlockEntityBlockEventHandler, IScopedSerialization, IFragmentHost {
    @JvmField
    protected val fragList: BlockEntityFragmentCollection = BlockEntityFragmentCollection(this)

    override fun toMinecraft(): BlockEntity = this

    override fun blockEntity(): IBlockEntity = this

    override fun markDirty() = setChanged()

    override fun markDirtyAndSync() {
        setChanged()
        val lvl = level
        if (lvl != null && !lvl.isClientSide && handlesScope(NBTSerializationScope.DESCRIPTION)) {
            val state = blockState
            lvl.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS)
        }
    }

    override fun <T : Any> getModule(module: IModule<T>, side: Direction?): T? = fragList.getModule(module, side)

    /**
     * Capability provider target. Register with [com.itszuvalex.itszulib.api.ModuleCapabilities].
     */
    fun <T : Any> getCapability(cap: BlockCapability<T, Direction?>, side: Direction?): T? = fragList.getCapability(cap, side)

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        serializeTo(NBTSerializationScope.LEVEL, output)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        deserialize(input, NBTSerializationScope.LEVEL)
    }

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) =
        fragList.serializeTo(scope, output.child(FRAG_KEY))

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) {
        input.child(FRAG_KEY).ifPresent { fragList.deserialize(it, scope) }
        // Fragments may have replaced the objects they expose.
        if (level != null) invalidateCapabilities()
    }

    override fun handlesScope(scope: NBTSerializationScope): Boolean = fragList.handlesScope(scope)

    override fun clearRemoved() {
        fragList.rehydrateFrags()
        super.clearRemoved()
    }

    override fun setRemoved() {
        fragList.invalidateFrags()
        super.setRemoved()
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? =
        if (handlesScope(NBTSerializationScope.DESCRIPTION)) ClientboundBlockEntityDataPacket.create(this) else null

    override fun onDataPacket(net: Connection, input: ValueInput) {
        if (!handlesScope(NBTSerializationScope.DESCRIPTION)) return
        deserialize(input, NBTSerializationScope.DESCRIPTION)
    }

    override fun handleUpdateTag(input: ValueInput) = deserialize(input, NBTSerializationScope.DESCRIPTION)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag =
        ProblemReporter.ScopedCollector(problemPath(), LOGGER).use { reporter ->
            val output = TagValueOutput.createWithContext(reporter, registries)
            serializeTo(NBTSerializationScope.DESCRIPTION, output)
            output.buildResult()
        }

    /**
     * Carries ITEM-scope fragment data on the block's item form (loot `copy_components`, creative pick-block).
     */
    override fun collectImplicitComponents(components: DataComponentMap.Builder) {
        super.collectImplicitComponents(components)
        if (!handlesScope(NBTSerializationScope.ITEM)) return
        // Nothing to carry: leave the item plain, so empty blocks stack with fresh ones.
        val others = fragList.fragments().any { it.handlesScope(NBTSerializationScope.ITEM) && it !is IBreakContents }
        if (!others && !keepsContents()) return
        ProblemReporter.ScopedCollector(problemPath(), LOGGER).use { reporter ->
            val output = TagValueOutput.createWithContext(reporter, registries())
            serializeTo(NBTSerializationScope.ITEM, output)
            components.set(Components.FRAGMENT_DATA.get(), CustomData.of(output.buildResult()))
        }
    }

    /**
     * Restores ITEM-scope fragment data when the block is placed from an item carrying it.
     */
    override fun applyImplicitComponents(components: DataComponentGetter) {
        super.applyImplicitComponents(components)
        val data = components.get(Components.FRAGMENT_DATA.get())
        if (data == null || !handlesScope(NBTSerializationScope.ITEM)) return
        ProblemReporter.ScopedCollector(problemPath(), LOGGER).use { reporter ->
            deserialize(TagValueInput.create(reporter, registries(), data.copyTag()), NBTSerializationScope.ITEM)
        }
    }

    private fun registries(): HolderLookup.Provider = level?.registryAccess() ?: RegistryAccess.EMPTY

    /**
     * The fragments that hold contents and say what breaking the block does with them ([BreakBehavior]).
     */
    fun contentFragments(): List<IBreakContents> = fragList.fragments().filterIsInstance<IBreakContents>()

    /**
     * Whether breaking this block now would carry contents on the dropped item: some fragment keeps its contents
     * ([BreakBehavior.KEEP]) and holds something.
     */
    fun keepsContents(): Boolean = contentFragments().any { it.breakBehavior == BreakBehavior.KEEP && !it.isContentEmpty() }

    override fun preRemoveSideEffects(pos: BlockPos, state: BlockState) {
        super.preRemoveSideEffects(pos, state)
        level?.let { onRemove(ILevel.of(it), pos, state) }
    }

    override fun onRemove(level: ILevel, pos: BlockPos, blockStatePrev: BlockState) =
        fragList.onRemove(level, pos, blockStatePrev)

    override fun onLoad() {
        super<BlockEntity>.onLoad()
        level?.let { onLoad(ILevel.of(it), worldPosition) }
    }

    override fun onLoad(level: ILevel, pos: BlockPos) = fragList.onLoad(level, pos)

    override fun onChunkUnloaded() {
        super<BlockEntity>.onChunkUnloaded()
        level?.let { onChunkUnloaded(ILevel.of(it), worldPosition) }
    }

    override fun onChunkUnloaded(level: ILevel, pos: BlockPos) = fragList.onChunkUnloaded(level, pos)

    override fun onNeighborChanged(level: ILevel, pos: BlockPos) = fragList.onNeighborChanged(level, pos)

    companion object {
        private val LOGGER = LogUtils.getLogger()
        const val FRAG_KEY = "frags"
    }
}

abstract class TickableBlockEntityCore(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    BlockEntityCore(type, pos, state), IBlockEntityTickable {
    override fun tick(level: ILevel, blockPos: BlockPos, blockState: BlockState) = fragList.tick(level, blockPos, blockState)
}

abstract class EntityBlockCore<T : BlockEntity>(
    properties: BlockBehaviour.Properties,
    protected val typeSupplier: () -> BlockEntityType<T>,
) : Block(properties), EntityBlock {
    /**
     * Opens the menu the block entity exposes through [Modules.MENU] on the clicked face, if any.
     */
    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        val menu = (level.getBlockEntity(pos) as? IBlockEntity)?.getModule(Modules.MENU, hitResult.direction)
            ?: return super.useWithoutItem(state, level, pos, player, hitResult)
        if (player is ServerPlayer) player.openMenu(menu, menu.menuPos())
        return InteractionResult.SUCCESS
    }

    /**
     * Puts the block entity's kept contents ([BreakBehavior.KEEP]) on the dropped block item, whatever the block's loot
     * table says, so a block cannot forget to; the block entity's [BlockEntityCore.collectImplicitComponents] decides
     * whether there is anything to carry.
     */
    override fun getDrops(state: BlockState, params: LootParams.Builder): List<ItemStack> {
        val drops = super.getDrops(state, params)
        val be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) as? BlockEntityCore ?: return drops
        val data = be.collectComponents().get(Components.FRAGMENT_DATA.get()) ?: return drops
        return drops.map { stack ->
            if (stack.`is`(asItem()) && !stack.has(Components.FRAGMENT_DATA.get())) stack.copy().also { it.set(Components.FRAGMENT_DATA.get(), data) } else stack
        }
    }

    /**
     * Forwards to the block entity's [IBlockEntityBlockEventHandler.onNeighborChanged].
     */
    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, orientation: Orientation?, movedByPiston: Boolean) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston)
        (level.getBlockEntity(pos) as? IBlockEntityBlockEventHandler)?.onNeighborChanged(ILevel.of(level), pos)
    }
}

abstract class TickableEntityBlockCore<T : TickableBlockEntityCore>(
    properties: BlockBehaviour.Properties,
    typeSupplier: () -> BlockEntityType<T>,
) : EntityBlockCore<T>(properties, typeSupplier) {
    override fun <E : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<E>): BlockEntityTicker<E>? {
        if (type !== typeSupplier()) return null
        if (!hasTicker(SidedHelper.sideFromIsClient(level.isClientSide))) return null
        return BlockEntityTicker { lvl, pos, st, be -> (be as IBlockEntityTickable).tick(ILevel.of(lvl), pos, st) }
    }

    /**
     * @return Whether block entities of this block tick on [side].
     */
    open fun hasTicker(side: LogicalSide): Boolean = false
}
