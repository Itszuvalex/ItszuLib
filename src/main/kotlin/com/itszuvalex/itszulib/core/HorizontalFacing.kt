package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.utility.DirectionUtil
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty

/**
 * A horizontal `facing` block state property: the block's front, set to face the player who placed it, and kept
 * through structure rotation and mirroring. Port of ItszuLib 1.12.2's `BlockBehaviorHorizontalFacing`. Used by
 * [HorizontalEntityBlockCore] and [TickableHorizontalEntityBlockCore], which differ only in their base class.
 *
 * 1.12.2 also turned a block placed without a player away from an adjacent full block; that is not ported (vanilla
 * dropped the same behaviour from furnaces), so such blocks keep the default north facing.
 */
object HorizontalFacing {
    @JvmField
    val FACING: EnumProperty<Direction> = BlockStateProperties.HORIZONTAL_FACING

    /**
     * The front of a block in [state]: its `facing`, or [DirectionUtil.DEFAULT_HORIZONTAL_FACING] if it has none. Use
     * as a [SidedStorageConfiguration]'s front, e.g. `{ HorizontalFacing.front(blockState) }`.
     */
    @JvmStatic
    fun front(state: BlockState): Direction =
        if (state.hasProperty(FACING)) state.getValue(FACING) else DirectionUtil.DEFAULT_HORIZONTAL_FACING

    @JvmStatic
    fun placementState(defaultState: BlockState, context: BlockPlaceContext): BlockState =
        defaultState.setValue(FACING, context.horizontalDirection.opposite)

    @JvmStatic
    fun rotate(state: BlockState, rotation: Rotation): BlockState = state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    @JvmStatic
    fun mirror(state: BlockState, mirror: Mirror): BlockState = state.rotate(mirror.getRotation(state.getValue(FACING)))
}

/**
 * An [EntityBlockCore] with a [HorizontalFacing] front.
 */
abstract class HorizontalEntityBlockCore<T : BlockEntity>(properties: BlockBehaviour.Properties, typeSupplier: () -> BlockEntityType<T>) :
    EntityBlockCore<T>(properties, typeSupplier) {
    init {
        registerDefaultState(stateDefinition.any().setValue(HorizontalFacing.FACING, DirectionUtil.DEFAULT_HORIZONTAL_FACING))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(HorizontalFacing.FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? = HorizontalFacing.placementState(defaultBlockState(), context)

    override fun rotate(state: BlockState, rotation: Rotation): BlockState = HorizontalFacing.rotate(state, rotation)

    override fun mirror(state: BlockState, mirror: Mirror): BlockState = HorizontalFacing.mirror(state, mirror)
}

/**
 * A [TickableEntityBlockCore] with a [HorizontalFacing] front.
 */
abstract class TickableHorizontalEntityBlockCore<T : TickableBlockEntityCore>(
    properties: BlockBehaviour.Properties,
    typeSupplier: () -> BlockEntityType<T>,
) : TickableEntityBlockCore<T>(properties, typeSupplier) {
    init {
        registerDefaultState(stateDefinition.any().setValue(HorizontalFacing.FACING, DirectionUtil.DEFAULT_HORIZONTAL_FACING))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(HorizontalFacing.FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? = HorizontalFacing.placementState(defaultBlockState(), context)

    override fun rotate(state: BlockState, rotation: Rotation): BlockState = HorizontalFacing.rotate(state, rotation)

    override fun mirror(state: BlockState, mirror: Mirror): BlockState = HorizontalFacing.mirror(state, mirror)
}
