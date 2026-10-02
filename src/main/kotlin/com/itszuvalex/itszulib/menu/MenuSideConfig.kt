package com.itszuvalex.itszulib.menu

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.utility.DirectionUtil
import com.itszuvalex.itszulib.core.BlockEntityCore
import com.itszuvalex.itszulib.core.EnumAutomaticIO
import com.itszuvalex.itszulib.core.SidedStorageConfiguration
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
 * Screens read the configurations from the client block entity, which syncs them.
 */
class MenuSideConfig(@JvmField val blockEntity: BlockEntity, modes: List<SideConfigMode>, private val onChanged: () -> Unit) {
    @JvmField
    val modes: List<SideConfigMode> = modes.filter { configuration(it) != null }

    fun configuration(mode: SideConfigMode): SidedStorageConfiguration<*>? = (blockEntity as? IBlockEntity)?.getModule(mode.module, null)

    /**
     * Server side: applies [ACTION_SIDE_CONFIG] data from [data].
     *
     * @return False for a bad face or mode.
     */
    fun handle(data: Int): Boolean {
        val face = data and FACE_MASK
        if (face >= Direction.entries.size) return false
        val mode = modes.getOrNull((data shr MODE_SHIFT) and MODE_MASK) ?: return false
        val config = configuration(mode) ?: return false
        mode.cycler.cycle(config, Direction.from3DDataValue(face), data and BACKWARD != 0)
        onChanged()
        return true
    }

    companion object {
        private const val FACE_MASK = 7
        private const val MODE_SHIFT = 3
        private const val MODE_MASK = 255
        private const val BACKWARD = 1 shl 11

        /**
         * Action data: cycle [face] of the configuration of mode index [mode] (into [modes]).
         */
        @JvmStatic
        fun data(face: Direction, mode: Int, backward: Boolean): Int =
            face.get3DDataValue() or ((mode and MODE_MASK) shl MODE_SHIFT) or (if (backward) BACKWARD else 0)

        /**
         * Saves and syncs a [BlockEntityCore] (the default after a change).
         */
        @JvmStatic
        fun markDirtyAndSync(be: BlockEntity) {
            (be as? BlockEntityCore)?.markDirtyAndSync() ?: be.setChanged()
        }
    }
}
