package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.storage.IFluidStorage
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.utility.ChunkCoord
import com.itszuvalex.itszulib.api.utility.DirectionUtil
import com.itszuvalex.itszulib.core.EnumAutomaticIO
import com.itszuvalex.itszulib.core.SidedFluidStorageConfiguration
import com.itszuvalex.itszulib.core.SidedItemStorageConfiguration
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.neoforge.common.util.ValueIOSerializable

/**
 * Multiblock membership of one block: whether it is part of a formed multiblock, and where that multiblock's
 * controller is. Port of ItszuLib 1.12.2's `MultiBlockInfo`; exposed through [Modules.MULTIBLOCK] by
 * [com.itszuvalex.itszulib.core.frag.FragMultiBlockInfo].
 *
 * Persisted as `isFormed`, `c_loc` (controller position) and `controller` (whether this block is the controller).
 */
class MultiBlockInfo : ValueIOSerializable {
    /**
     * Run after [form] or [breakFrom] change this info, e.g. to save and sync the owning block entity.
     */
    var onChanged: Runnable = Runnable {}

    var isFormed: Boolean = false
        private set

    var isController: Boolean = false
        private set

    private var controllerPos: BlockPos = BlockPos.ZERO

    /**
     * Controller position while formed, otherwise null.
     */
    val controller: BlockPos? get() = if (isFormed) controllerPos else null

    /**
     * Joins the multiblock controlled at [controller].
     *
     * @param self Position of the block holding this info.
     * @return False if already part of a different multiblock.
     */
    fun form(self: BlockPos, controller: BlockPos): Boolean {
        if (isFormed && controller != controllerPos) return false
        isFormed = true
        controllerPos = controller.immutable()
        isController = self == controller
        onChanged.run()
        return true
    }

    /**
     * Leaves the multiblock controlled at [controller].
     *
     * @return False if part of a different multiblock.
     */
    fun breakFrom(controller: BlockPos): Boolean {
        if (isFormed && controller != controllerPos) return false
        isFormed = false
        isController = false
        onChanged.run()
        return true
    }

    override fun serialize(output: ValueOutput) {
        output.putBoolean(FORMED_KEY, isFormed)
        output.store(CONTROLLER_LOC_KEY, BlockPos.CODEC, controllerPos)
        output.putBoolean(CONTROLLER_KEY, isController)
    }

    override fun deserialize(input: ValueInput) {
        isFormed = input.getBooleanOr(FORMED_KEY, false)
        controllerPos = input.read(CONTROLLER_LOC_KEY, BlockPos.CODEC).orElse(BlockPos.ZERO)
        isController = input.getBooleanOr(CONTROLLER_KEY, false)
    }

    companion object {
        const val FORMED_KEY = "isFormed"
        const val CONTROLLER_LOC_KEY = "c_loc"
        const val CONTROLLER_KEY = "controller"
    }
}

/**
 * The blocks a multiblock is made of, as offsets from its controller.
 */
interface IBlockPattern {
    val rotation: Rotation

    /**
     * @return A copy of this pattern rotated by [rot] around the controller (offset (0,0,0)).
     */
    fun rotated(rot: Rotation): IBlockPattern

    /**
     * @return True if the blocks around a controller at [pos] form this pattern.
     */
    fun matches(level: Level, pos: BlockPos): Boolean

    /**
     * @return World positions of every block of a multiblock with its controller at [pos].
     */
    fun blocksInMatch(pos: BlockPos): Collection<BlockPos>

    fun blocksIfMatch(level: Level, pos: BlockPos): Collection<BlockPos>? = if (matches(level, pos)) blocksInMatch(pos) else null

    /**
     * @return True if a multiblock with its controller at [pos] would span more than one chunk.
     */
    fun overChunkBoundaries(pos: BlockPos): Boolean {
        val blocks = blocksInMatch(pos)
        val first = blocks.firstOrNull() ?: return false
        val chunk = ChunkCoord.of(first)
        return blocks.any { ChunkCoord.of(it) != chunk }
    }
}

/**
 * A pattern of fixed blocks. Immutable: [rotated] returns a new pattern.
 *
 * @param blocks Offset from the controller to the block required there. (0,0,0) is the controller.
 */
class BlockPatternStatic @JvmOverloads constructor(
    blocks: Map<BlockPos, Block>,
    override val rotation: Rotation = Rotation.NONE,
) : IBlockPattern {
    val blocks: Map<BlockPos, Block> = blocks.toMap()

    override fun rotated(rot: Rotation): IBlockPattern =
        BlockPatternStatic(blocks.mapKeys { (offset, _) -> offset.rotate(rot) }, rotation.getRotated(rot))

    override fun matches(level: Level, pos: BlockPos): Boolean =
        blocks.all { (offset, block) -> level.getBlockState(pos.offset(offset)).`is`(block) }

    override fun blocksInMatch(pos: BlockPos): Collection<BlockPos> = blocks.keys.map { pos.offset(it) }
}

/**
 * Forms and breaks a multiblock: tells every block in the pattern (through its [Modules.MULTIBLOCK] module) which
 * controller it belongs to.
 */
interface IMultiblock {
    val pattern: IBlockPattern

    /**
     * Assumes [pat] matches at [pos].
     *
     * @return True if every block of the pattern joined. False if any block has no multiblock module or already belongs
     * to another multiblock; the other blocks still join.
     */
    fun form(level: ILevel, pos: BlockPos, pat: IBlockPattern): Boolean

    /**
     * @return Null if [pat] does not match at [pos]; otherwise the result of [form].
     */
    fun tryForm(level: Level, pos: BlockPos, pat: IBlockPattern): Boolean? =
        if (!pat.matches(level, pos)) null else form(ILevel.of(level), pos, pat)

    /**
     * @return True if every block of the pattern left. False if any block has no multiblock module or belongs to
     * another multiblock.
     */
    fun breakMultiblock(level: ILevel, pos: BlockPos, pat: IBlockPattern): Boolean
}

open class MultiblockStatic(private val blockPattern: IBlockPattern) : IMultiblock {
    override val pattern: IBlockPattern get() = blockPattern

    override fun form(level: ILevel, pos: BlockPos, pat: IBlockPattern): Boolean =
        forEachInfo(level, pos, pat) { at, info -> info.form(at, pos) }

    override fun breakMultiblock(level: ILevel, pos: BlockPos, pat: IBlockPattern): Boolean =
        forEachInfo(level, pos, pat) { _, info -> info.breakFrom(pos) }

    private fun forEachInfo(level: ILevel, pos: BlockPos, pat: IBlockPattern, action: (BlockPos, MultiBlockInfo) -> Boolean): Boolean {
        var ok = true
        for (at in pat.blocksInMatch(pos)) {
            val info = level.getIBlockEntity(at)?.getModule(Modules.MULTIBLOCK, null)
            // Keep going after a failure, so as many blocks as possible end up consistent.
            ok = (info != null && action(at, info)) && ok
        }
        return ok
    }
}

object MultiblockUtils {
    /**
     * @return True if the block on [facing] of [pos] belongs to the same formed multiblock as [info].
     */
    @JvmStatic
    fun isFacingInMultiblock(level: ILevel, pos: BlockPos, facing: Direction, info: MultiBlockInfo): Boolean {
        val controller = info.controller ?: return false
        val neighbour = pos.relative(facing)
        // Never load a chunk to answer this (1.12.2 looked up with force = false): a block entity lookup in an
        // unloaded chunk loads it synchronously on the server.
        if (!level.isLoaded(neighbour)) return false
        val other = level.getIBlockEntity(neighbour)?.getModule(Modules.MULTIBLOCK, null) ?: return false
        return other.controller == controller
    }

    /**
     * @return The block entity of [info]'s controller, if formed and loaded.
     */
    @JvmStatic
    fun controller(level: ILevel, info: MultiBlockInfo) = info.controller?.let { if (level.isLoaded(it)) level.getIBlockEntity(it) else null }
}

/**
 * Decides which faces of a multiblock part touch the rest of its multiblock. Shared by
 * [MultiblockSidedItemStorageConfiguration] and [MultiblockSidedFluidStorageConfiguration].
 */
class MultiblockFaces(
    private val level: () -> ILevel?,
    private val pos: () -> BlockPos,
    private val info: MultiBlockInfo,
    private val front: () -> Direction,
) {
    fun internal(absolute: Direction): Boolean = level()?.let { MultiblockUtils.isFacingInMultiblock(it, pos(), absolute, info) } ?: false

    fun internalRelative(relative: Direction): Boolean = internal(DirectionUtil.getAbsoluteDirectionFromHorizontalRelative(relative, front()))
}

/**
 * Sided item configuration of a multiblock part: faces touching the rest of the same multiblock expose
 * [emptyStorage]'s storage (normally an empty one), do no automatic IO and cannot be reconfigured. Port of ItszuLib
 * 1.12.2's `MultiblockSidedItemStorageConfiguration`.
 *
 * Relative and absolute queries agree: an internal face reports [emptyStorage] and [EnumAutomaticIO.NONE] either way
 * (1.12.2 only overrode the absolute queries, so a screen showed the stored setting of a face that did nothing).
 */
open class MultiblockSidedItemStorageConfiguration(
    level: () -> ILevel?,
    pos: () -> BlockPos,
    info: MultiBlockInfo,
    private val emptyStorage: String,
    defaults: (Direction) -> String,
    storages: Map<String, IItemStorage>,
    front: () -> Direction,
) : SidedItemStorageConfiguration(defaults, storages, front) {
    private val faces = MultiblockFaces(level, pos, info, front)

    override fun getStorageNameForAbsoluteFacing(direction: Direction): String =
        if (faces.internal(direction)) emptyStorage else super.getStorageNameForAbsoluteFacing(direction)

    override fun getStorageNameForRelativeFacing(direction: Direction): String =
        if (faces.internalRelative(direction)) emptyStorage else super.getStorageNameForRelativeFacing(direction)

    override fun getIOForAbsoluteFacing(direction: Direction): EnumAutomaticIO =
        if (faces.internal(direction)) EnumAutomaticIO.NONE else super.getIOForAbsoluteFacing(direction)

    override fun getIOForRelativeFacing(direction: Direction): EnumAutomaticIO =
        if (faces.internalRelative(direction)) EnumAutomaticIO.NONE else super.getIOForRelativeFacing(direction)

    override fun cycleRelativeFacingStorageForward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingStorageForward(direction)
    }

    override fun cycleRelativeFacingStorageBackward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingStorageBackward(direction)
    }

    override fun cycleRelativeFacingIOForward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingIOForward(direction)
    }

    override fun cycleRelativeFacingIOBackward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingIOBackward(direction)
    }
}

/**
 * Fluid counterpart of [MultiblockSidedItemStorageConfiguration].
 */
open class MultiblockSidedFluidStorageConfiguration(
    level: () -> ILevel?,
    pos: () -> BlockPos,
    info: MultiBlockInfo,
    private val emptyStorage: String,
    defaults: (Direction) -> String,
    storages: Map<String, IFluidStorage>,
    front: () -> Direction,
) : SidedFluidStorageConfiguration(defaults, storages, front) {
    private val faces = MultiblockFaces(level, pos, info, front)

    override fun getStorageNameForAbsoluteFacing(direction: Direction): String =
        if (faces.internal(direction)) emptyStorage else super.getStorageNameForAbsoluteFacing(direction)

    override fun getStorageNameForRelativeFacing(direction: Direction): String =
        if (faces.internalRelative(direction)) emptyStorage else super.getStorageNameForRelativeFacing(direction)

    override fun getIOForAbsoluteFacing(direction: Direction): EnumAutomaticIO =
        if (faces.internal(direction)) EnumAutomaticIO.NONE else super.getIOForAbsoluteFacing(direction)

    override fun getIOForRelativeFacing(direction: Direction): EnumAutomaticIO =
        if (faces.internalRelative(direction)) EnumAutomaticIO.NONE else super.getIOForRelativeFacing(direction)

    override fun cycleRelativeFacingStorageForward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingStorageForward(direction)
    }

    override fun cycleRelativeFacingStorageBackward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingStorageBackward(direction)
    }

    override fun cycleRelativeFacingIOForward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingIOForward(direction)
    }

    override fun cycleRelativeFacingIOBackward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingIOBackward(direction)
    }
}
