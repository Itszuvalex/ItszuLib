package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.multiblock.BlockPatternStatic
import com.itszuvalex.itszulib.api.multiblock.MultiblockSidedItemStorageConfiguration
import com.itszuvalex.itszulib.api.multiblock.MultiblockStatic
import com.itszuvalex.itszulib.api.storage.FluidStorageArray
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.storage.ItemStorageArray
import com.itszuvalex.itszulib.api.storage.ItemStorageSlice
import com.itszuvalex.itszulib.core.SidedFluidStorageConfiguration
import com.itszuvalex.itszulib.core.SidedItemStorageConfiguration
import com.itszuvalex.itszulib.core.TickableBlockEntityCore
import com.itszuvalex.itszulib.core.HorizontalFacing
import com.itszuvalex.itszulib.core.TickableEntityBlockCore
import com.itszuvalex.itszulib.core.TickableHorizontalEntityBlockCore
import com.itszuvalex.itszulib.core.frag.FragMenu
import net.minecraft.network.chat.Component
import com.itszuvalex.itszulib.core.frag.FragFluidAutoIO
import com.itszuvalex.itszulib.core.frag.FragFluidStorage
import com.itszuvalex.itszulib.core.frag.FragItemAutoIO
import com.itszuvalex.itszulib.core.frag.FragItemStorage
import com.itszuvalex.itszulib.core.frag.FragMultiBlockInfo
import com.itszuvalex.itszulib.core.frag.FragMultiblockState
import com.itszuvalex.itszulib.core.frag.FragMultiblockTickable
import com.itszuvalex.itszulib.core.frag.FragSidedConfiguration
import com.itszuvalex.itszulib.core.frag.addFluidStorage
import com.itszuvalex.itszulib.core.frag.addItemStorage
import com.itszuvalex.itszulib.core.frag.addTickableFragment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.fml.LogicalSide
import net.neoforged.neoforge.common.util.ValueIOSerializable

/**
 * A machine-like block: a two-slot inventory (slot 0 "input", slot 1 "output") and one 4000 mB tank, each with a
 * sided configuration (every face starts on "input" / "tank" with no automatic IO) and automatic IO every tick.
 */
class DevMachineBlock(properties: BlockBehaviour.Properties) :
    TickableHorizontalEntityBlockCore<DevMachineBlockEntity>(properties, { DevContent.DEV_MACHINE_BLOCK_ENTITY.get() }) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = DevMachineBlockEntity(pos, state)

    override fun hasTicker(side: LogicalSide): Boolean = side == LogicalSide.SERVER
}

class DevMachineBlockEntity(pos: BlockPos, state: BlockState) :
    TickableBlockEntityCore(DevContent.DEV_MACHINE_BLOCK_ENTITY.get(), pos, state) {
    @JvmField
    val inventory = ItemStorageArray(2) { markDirty() }

    @JvmField
    val input: IItemStorage = ItemStorageSlice(inventory, intArrayOf(0))

    @JvmField
    val output: IItemStorage = ItemStorageSlice(inventory, intArrayOf(1))

    @JvmField
    val tanks = FluidStorageArray(1, TANK_CAPACITY) { markDirty() }

    @JvmField
    val itemConfig = FragSidedConfiguration(
        "ItemConfig",
        SidedItemStorageConfiguration({ "input" }, mapOf("input" to input, "output" to output), { HorizontalFacing.front(blockState) }),
        Modules.ITEM_STORAGE_CONFIGURABLE,
    )

    @JvmField
    val fluidConfig = FragSidedConfiguration(
        "FluidConfig",
        SidedFluidStorageConfiguration({ "tank" }, mapOf("tank" to tanks), { HorizontalFacing.front(blockState) }),
        Modules.FLUID_STORAGE_CONFIGURABLE,
    )

    init {
        fragList.addFragment(itemConfig)
        fragList.addFragment(fluidConfig)
        fragList.addItemStorage(FragItemStorage(inventory))
        fragList.addFluidStorage(FragFluidStorage(tanks))
        fragList.addTickableFragment(FragItemAutoIO({ 1 }, { ITEMS_PER_TICK }))
        fragList.addTickableFragment(FragFluidAutoIO({ 1 }, { MB_PER_TICK }))
        fragList.addFragment(FragMenu(Component.literal("Dev machine"), { id, inv, _ -> DevMenu(id, inv, this) }))
    }

    companion object {
        const val TANK_CAPACITY = 4000
        const val ITEMS_PER_TICK = 4
        const val MB_PER_TICK = 100
    }
}

/**
 * Multiblock state of [DevMultiblockBlockEntity]: counts server ticks while formed.
 */
class DevCounter : ValueIOSerializable {
    var count = 0

    override fun serialize(output: ValueOutput) = output.putInt("count", count)

    override fun deserialize(input: ValueInput) {
        count = input.getIntOr("count", 0)
    }
}

/**
 * A part of a two-block multiblock ([PATTERN]: the controller and the block east of it). Each part has a one-slot
 * inventory whose faces touching the other part expose nothing; the controller counts ticks in its multiblock state.
 */
class DevMultiblockBlock(properties: BlockBehaviour.Properties) :
    TickableEntityBlockCore<DevMultiblockBlockEntity>(properties, { DevContent.DEV_MULTIBLOCK_BLOCK_ENTITY.get() }) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = DevMultiblockBlockEntity(pos, state)

    override fun hasTicker(side: LogicalSide): Boolean = side == LogicalSide.SERVER

    companion object {
        @JvmStatic
        val PATTERN: BlockPatternStatic by lazy {
            BlockPatternStatic(mapOf(BlockPos.ZERO to DevContent.DEV_MULTIBLOCK_BLOCK.get(), BlockPos(1, 0, 0) to DevContent.DEV_MULTIBLOCK_BLOCK.get()))
        }

        @JvmStatic
        val MULTIBLOCK: MultiblockStatic by lazy { MultiblockStatic(PATTERN) }
    }
}

class DevMultiblockBlockEntity(pos: BlockPos, state: BlockState) :
    TickableBlockEntityCore(DevContent.DEV_MULTIBLOCK_BLOCK_ENTITY.get(), pos, state) {
    @JvmField
    val info = FragMultiBlockInfo()

    @JvmField
    val mbState: FragMultiblockState<DevCounter> =
        FragMultiblockState(info, ::DevCounter, { other -> (other as? DevMultiblockBlockEntity)?.mbState })

    @JvmField
    val inventory = ItemStorageArray(1) { markDirty() }

    @JvmField
    val itemConfig = FragSidedConfiguration<SidedItemStorageConfiguration>(
        "ItemConfig",
        MultiblockSidedItemStorageConfiguration(
            { level?.let(ILevel::of) }, { blockPos }, info.info, "empty", { "main" },
            mapOf("main" to inventory, "empty" to IItemStorage.Empty), { Direction.NORTH },
        ),
        Modules.ITEM_STORAGE_CONFIGURABLE,
    )

    init {
        fragList.addFragment(info)
        fragList.addInternalFragment(mbState)
        fragList.addFragment(itemConfig)
        fragList.addItemStorage(FragItemStorage(inventory))
        fragList.addFragment(FragMenu(Component.literal("Dev multiblock"), { id, inv, _ -> DevMenu(id, inv, this) }, info))
        fragList.addTickableFragment(object : FragMultiblockTickable(info.info) {
            override fun name(): String = "Counter"

            override fun serverControllerTick(level: ILevel, pos: BlockPos) {
                mbState.doIfController { it.count++ }
            }
        })
    }
}
