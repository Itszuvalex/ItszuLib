package com.itszuvalex.itszulib.menu

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.utility.DirectionUtil
import com.itszuvalex.itszulib.core.BlockEntityCore
import com.itszuvalex.itszulib.core.EnumAutomaticIO
import com.itszuvalex.itszulib.core.SidedStorageConfiguration
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * Changes one face of a sided configuration. [face] is absolute; configurations store faces relative to their front.
 */
fun interface SideConfigCycler {
    fun cycle(config: SidedStorageConfiguration<*>, face: Direction, backward: Boolean)
}

object SideConfigCyclers {
    /**
     * Cycles the face's automatic IO (none, input, output).
     */
    @JvmField
    val IO = SideConfigCycler { config, face, backward ->
        val relative = relative(config, face)
        if (backward) config.cycleRelativeFacingIOBackward(relative) else config.cycleRelativeFacingIOForward(relative)
    }

    /**
     * Cycles which storage the face uses.
     */
    @JvmField
    val STORAGE = SideConfigCycler { config, face, backward ->
        val relative = relative(config, face)
        if (backward) config.cycleRelativeFacingStorageBackward(relative) else config.cycleRelativeFacingStorageForward(relative)
    }

    /**
     * Walks every IO and storage combination with one control: forward cycles the IO and, when it wraps to NONE, the
     * storage; backward cycles the IO back and, when it lands on OUTPUT (wrapped from NONE), the storage back.
     */
    @JvmField
    val IO_THEN_STORAGE = SideConfigCycler { config, face, backward ->
        val relative = relative(config, face)
        if (backward) {
            config.cycleRelativeFacingIOBackward(relative)
            if (config.getIOForAbsoluteFacing(face) == EnumAutomaticIO.OUTPUT) config.cycleRelativeFacingStorageBackward(relative)
        } else {
            config.cycleRelativeFacingIOForward(relative)
            if (config.getIOForAbsoluteFacing(face) == EnumAutomaticIO.NONE) config.cycleRelativeFacingStorageForward(relative)
        }
    }

    private fun relative(config: SidedStorageConfiguration<*>, face: Direction): Direction =
        DirectionUtil.getHorizontalRelativeDirectionFromAbsolute(face, config.front())
}

/**
 * One kind of sided configuration a screen can edit: the module it is exposed as, a label and how a click changes a
 * face.
 */
class SideConfigMode @JvmOverloads constructor(
    @JvmField val id: Identifier,
    @JvmField val label: Component,
    @JvmField val module: IModule<out SidedStorageConfiguration<*>>,
    @JvmField val cycler: SideConfigCycler = SideConfigCyclers.IO,
) {
    /**
     * This mode with another [cycler].
     */
    fun withCycler(cycler: SideConfigCycler) = SideConfigMode(id, label, module, cycler)
}

/**
 * The item, fluid and energy configurations ([Modules.ITEM_STORAGE_CONFIGURABLE], [Modules.FLUID_STORAGE_CONFIGURABLE],
 * [Modules.ENERGY_STORAGE_CONFIGURABLE]), cycling IO.
 */
object SideConfigModes {
    @JvmField
    val ITEM = mode("item", Modules.ITEM_STORAGE_CONFIGURABLE)

    @JvmField
    val FLUID = mode("fluid", Modules.FLUID_STORAGE_CONFIGURABLE)

    @JvmField
    val ENERGY = mode("energy", Modules.ENERGY_STORAGE_CONFIGURABLE)

    @JvmField
    val DEFAULTS: List<SideConfigMode> = listOf(ITEM, FLUID, ENERGY)

    private fun mode(name: String, module: IModule<out SidedStorageConfiguration<*>>) =
        SideConfigMode(Identifier.fromNamespaceAndPath(ItszuLib.ID, name), Component.translatable("gui.itszulib.side_config.mode.$name"), module)
}

/**
 * A menu's side configuration support (see [MenuCore.enableSideConfig]): the block entity, the modes it has (in the
 * order given, so both sides agree on indices), and the [MenuCore.ACTION_SIDE_CONFIG] action that cycles one face.
 * When the block entity is a member of a formed multiblock ([Modules.MULTIBLOCK_MEMBER]), the action reaches every
 * loaded member of the structure ([members]), so one screen configures the whole structure. Screens read the
 * configurations from the client block entities, which sync them. [onChanged] runs for the block entity whose
 * configuration changed.
 */
class MenuSideConfig(@JvmField val blockEntity: BlockEntity, modes: List<SideConfigMode>, private val onChanged: (BlockEntity) -> Unit) {
    @JvmField
    val modes: List<SideConfigMode> = modes.filter { configuration(it) != null }

    fun configuration(mode: SideConfigMode): SidedStorageConfiguration<*>? = (blockEntity as? IBlockEntity)?.getModule(mode.module, null)

    /**
     * The configuration of [mode] on the member at [pos] (one of [members]), or null.
     */
    fun configuration(mode: SideConfigMode, pos: BlockPos): SidedStorageConfiguration<*>? =
        if (pos == blockEntity.blockPos) configuration(mode) else (memberAt(pos) as? IBlockEntity)?.getModule(mode.module, null)

    /**
     * The blocks this side configuration covers: the block entity's own position first, then the other loaded members
     * of its formed multiblock (same structure id), in shape order. Just the block entity's position when it is not in
     * a formed structure.
     */
    fun members(): List<BlockPos> {
        val own = blockEntity.blockPos
        val membership = (blockEntity as? IBlockEntity)?.getModule(Modules.MULTIBLOCK_MEMBER, null)?.membership ?: return listOf(own)
        val level = blockEntity.level ?: return listOf(own)
        val anchor = own.subtract(membership.offset)
        return listOf(own) + membership.shape.positions(anchor).filter { pos ->
            pos != own && level.isLoaded(pos) &&
                (level.getBlockEntity(pos) as? IBlockEntity)?.getModule(Modules.MULTIBLOCK_MEMBER, null)?.membership?.structureId == membership.structureId
        }
    }

    private fun memberAt(pos: BlockPos): BlockEntity? =
        if (pos == blockEntity.blockPos) blockEntity else if (pos in members()) blockEntity.level?.getBlockEntity(pos) else null

    /**
     * Server side: applies [ACTION_SIDE_CONFIG] data from [data].
     *
     * @return False for a bad face, mode or member.
     */
    fun handle(data: Int): Boolean {
        val face = data and FACE_MASK
        if (face >= Direction.entries.size) return false
        val mode = modes.getOrNull((data shr MODE_SHIFT) and MODE_MASK) ?: return false
        val target = memberAt(blockEntity.blockPos.offset(offset(data))) ?: return false
        val config = (target as? IBlockEntity)?.getModule(mode.module, null) ?: return false
        mode.cycler.cycle(config, Direction.from3DDataValue(face), data and BACKWARD != 0)
        onChanged(target)
        return true
    }

    companion object {
        private const val FACE_MASK = 7
        private const val MODE_SHIFT = 3
        private const val MODE_MASK = 255
        private const val BACKWARD = 1 shl 11
        private const val OFFSET_SHIFT = 12
        private const val OFFSET_BITS = 6
        private const val OFFSET_MASK = (1 shl OFFSET_BITS) - 1

        /**
         * How far (in blocks, along each axis) a member may be from the menu's block entity to be configured through
         * it: `-MAX_OFFSET - 1 .. MAX_OFFSET`.
         */
        const val MAX_OFFSET = (1 shl (OFFSET_BITS - 1)) - 1

        /**
         * Action data: cycle [face] of the configuration of mode index [mode] (into [modes]) on the member at
         * [member] relative to the menu's block entity.
         */
        @JvmStatic
        @JvmOverloads
        fun data(face: Direction, mode: Int, backward: Boolean, member: BlockPos = BlockPos.ZERO): Int {
            require(listOf(member.x, member.y, member.z).all { it in -MAX_OFFSET - 1..MAX_OFFSET }) { "Member offset $member out of range" }
            val offset = (member.x and OFFSET_MASK) or ((member.y and OFFSET_MASK) shl OFFSET_BITS) or ((member.z and OFFSET_MASK) shl (2 * OFFSET_BITS))
            return face.get3DDataValue() or ((mode and MODE_MASK) shl MODE_SHIFT) or (if (backward) BACKWARD else 0) or (offset shl OFFSET_SHIFT)
        }

        /**
         * The member offset in action [data].
         */
        @JvmStatic
        fun offset(data: Int): BlockPos {
            fun axis(i: Int): Int {
                val v = (data ushr (OFFSET_SHIFT + i * OFFSET_BITS)) and OFFSET_MASK
                return if (v > MAX_OFFSET) v - (1 shl OFFSET_BITS) else v
            }
            return BlockPos(axis(0), axis(1), axis(2))
        }

        /**
         * Saves and syncs a [BlockEntityCore] (the default after a change).
         */
        @JvmStatic
        fun markDirtyAndSync(be: BlockEntity) {
            (be as? BlockEntityCore)?.markDirtyAndSync() ?: be.setChanged()
        }
    }
}
