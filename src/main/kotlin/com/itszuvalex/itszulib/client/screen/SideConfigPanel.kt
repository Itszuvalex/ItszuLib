package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.client.scene.BlockSceneBlock
import com.itszuvalex.itszulib.client.scene.BlockSceneGeometry
import com.itszuvalex.itszulib.client.scene.BlockSceneOverlay
import com.itszuvalex.itszulib.client.scene.BlockScenePickBox
import com.itszuvalex.itszulib.client.scene.BlockSceneView
import com.itszuvalex.itszulib.core.EnumAutomaticIO
import com.itszuvalex.itszulib.core.SidedStorageConfiguration
import com.itszuvalex.itszulib.menu.MenuActionPayload
import com.itszuvalex.itszulib.menu.MenuCore
import com.itszuvalex.itszulib.menu.MenuSideConfig
import com.itszuvalex.itszulib.menu.SideConfigMode
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.client.network.ClientPacketDistributor
import org.joml.Vector3f

/**
 * Side configuration in 3D (after Ender IO's IO configuration view): the block in the middle and its six neighbours
 * pulled out around it at half size, each face shaded by its automatic IO (blue pulls in, orange pushes out). Drag to
 * rotate (it starts with the front towards the player); hover a face for its direction, IO and storage; click a face,
 * or the neighbour on that side, to cycle it with the mode's [SideConfigMode.cycler] (shift cycles backwards); the
 * button switches between the menu's modes, and the small one beside it the view's background between dark and light
 * (whichever shows the blocks better; remembered in [ScreenThemeConfig.sideConfigLight]). Reads the configurations from the client block entity (they sync with
 * it) and changes them through [MenuCore.ACTION_SIDE_CONFIG].
 *
 * For a member of a formed multiblock ([MenuSideConfig.members]) it shows the whole structure instead, scaled to fit
 * and without neighbours: every member's outer faces are shaded by that member's configuration and clicking one
 * cycles it. Faces between members are not drawn (they cannot be configured).
 */
class SideConfigPanel(private val side: MenuSideConfig, private val containerId: Int) :
    ScreenComponent(VIEW + 2 * PAD, VIEW + 3 * PAD + BUTTON_H) {
    private val view = BlockSceneView(SCALE)
    private var modeIndex = 0
    private var initialized = false

    val mode: SideConfigMode get() = side.modes[modeIndex]

    override fun init(host: ComponentHost) {
        if (!initialized) {
            initialized = true
            side.configuration(mode)?.let { view.yaw = BlockSceneGeometry.defaultYaw(it.front()) }
        }
        view.x = x + PAD
        view.y = y + PAD
        view.width = VIEW
        view.height = VIEW
        val label = { Component.translatable("gui.itszulib.side_config.mode", mode.label) }
        val button = ThemedButton(x + PAD, y + 2 * PAD + VIEW, VIEW - BUTTON_H - PAD, BUTTON_H, label(), { b ->
            modeIndex = (modeIndex + 1) % side.modes.size
            b.message = label()
        })
        button.active = side.modes.size > 1
        host.addHostWidget(button)
        host.addHostWidget(
            ThemedButton(
                x + PAD + VIEW - BUTTON_H, y + 2 * PAD + VIEW, BUTTON_H, BUTTON_H, Component.literal("Bg"),
                { ScreenThemeConfig.setSideConfigLight(!ScreenThemeConfig.sideConfigLight()) },
                Component.translatable("gui.itszulib.side_config.background"),
                selected = ScreenThemeConfig::sideConfigLight,
            ),
        )
    }

    private fun neighbours(): List<Direction> {
        val be = side.blockEntity
        val level = be.level ?: return emptyList()
        return Direction.entries.filter { !level.getBlockState(be.blockPos.relative(it)).isAir }
    }

    private fun boxes(neighbours: List<Direction>): List<BlockScenePickBox<Direction?>> =
        listOf(BlockScenePickBox<Direction?>(null, Vector3f(), 1f)) +
            neighbours.map { BlockScenePickBox<Direction?>(it, neighbourCentre(it), NEIGHBOUR_SIZE) }

    /**
     * A face of one block the panel configures.
     */
    private data class Target(val pos: BlockPos, val face: Direction)

    /**
     * The face under the mouse: on a structure, the member face hit; on a single block, the face itself or the side of
     * the neighbour hit.
     */
    private fun targetAt(mx: Double, my: Double, members: List<BlockPos>): Target? {
        if (members.size > 1) {
            val centres = memberCentres(members)
            val hit = view.pick(mx, my, members.map { BlockScenePickBox(it, centres.getValue(it), 1f) }) ?: return null
            return Target(hit.key, hit.face)
        }
        val hit = view.pick(mx, my, boxes(neighbours())) ?: return null
        return Target(side.blockEntity.blockPos, hit.key ?: hit.face)
    }

    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        ScreenStyle.panel(graphics, x, y, width, height)
        graphics.fill(view.x, view.y, view.x + VIEW, view.y + VIEW, if (ScreenThemeConfig.sideConfigLight()) LIGHT_BACKGROUND else ScreenStyle.DARK)
        val be = side.blockEntity
        val level = be.level as? ClientLevel ?: return
        val members = side.members()
        view.scale = fitScale(members)
        val hovered = if (view.dragging) null else targetAt(mouseX.toDouble(), mouseY.toDouble(), members)
        val blocks: List<BlockSceneBlock>
        val overlays = ArrayList<BlockSceneOverlay>()
        if (members.size > 1) {
            val centres = memberCentres(members)
            val inStructure = members.toHashSet()
            blocks = members.map { BlockSceneBlock.of(level, it, centres.getValue(it), 1f) }
            for (pos in members) {
                val config = side.configuration(mode, pos)
                for (face in Direction.entries) {
                    if (pos.relative(face) in inStructure) continue
                    overlay(config, face, hovered == Target(pos, face), centres.getValue(pos))?.let(overlays::add)
                }
            }
        } else {
            val config = side.configuration(mode)
            blocks = listOf(BlockSceneBlock.of(level, be.blockPos, Vector3f(), 1f, be.blockState)) +
                neighbours().map { BlockSceneBlock.of(level, be.blockPos.relative(it), neighbourCentre(it), NEIGHBOUR_SIZE) }
            for (face in Direction.entries) overlay(config, face, hovered?.face == face, Vector3f())?.let(overlays::add)
        }
        view.submit(graphics, blocks, overlays)
        val config = hovered?.let { side.configuration(mode, it.pos) }
        if (hovered != null && config != null) {
            graphics.setTooltipForNextFrame(host.hostFont, listOf(
                Component.translatable("gui.itszulib.side_config.face", Component.translatable("gui.itszulib.side_config.dir.${hovered.face.serializedName}")),
                Component.translatable("gui.itszulib.side_config.io.${config.getIOForAbsoluteFacing(hovered.face).name.lowercase()}"),
                Component.translatable("gui.itszulib.side_config.storage", config.getStorageNameForAbsoluteFacing(hovered.face)),
            ), java.util.Optional.empty(), mouseX, mouseY)
        }
    }

    private fun overlay(config: SidedStorageConfiguration<*>?, face: Direction, hovered: Boolean, centre: Vector3f): BlockSceneOverlay? {
        val base = config?.let { colorFor(it.getIOForAbsoluteFacing(face)) } ?: 0
        val color = if (hovered) highlight(base) else base
        return if (color == 0) null else BlockSceneOverlay(centre, OVERLAY_SIZE, face, color)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean = view.press(event.x(), event.y())

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean = view.drag(event.x(), event.y(), dx, dy)

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val click = view.release() ?: return false
        if (!click) return true
        val target = targetAt(event.x(), event.y(), side.members()) ?: return true
        if (side.configuration(mode, target.pos) == null) return true
        val backward = Minecraft.getInstance().hasShiftDown()
        val member = target.pos.subtract(side.blockEntity.blockPos)
        ClientPacketDistributor.sendToServer(MenuActionPayload(containerId, MenuCore.ACTION_SIDE_CONFIG, MenuSideConfig.data(target.face, modeIndex, backward, member)))
        return true
    }

    companion object {
        const val VIEW = 112
        const val PAD = 4
        const val BUTTON_H = 14
        const val SCALE = 30f

        /** How much of the view a structure's bounding box may take. */
        const val FILL = 0.9f

        /**
         * Distance from the block's centre to a neighbour's centre, and a neighbour's edge, in blocks.
         */
        const val NEIGHBOUR_DISTANCE = 1.35f
        const val NEIGHBOUR_SIZE = 0.5f

        /**
         * Overlays sit a little outside the faces so they never fight them for depth.
         */
        const val OVERLAY_SIZE = 1.04f
        const val LIGHT_BACKGROUND = 0xFFE6E6E6.toInt()
        const val INPUT = 0x803399FF.toInt()
        const val OUTPUT = 0x80FF9933.toInt()

        @JvmStatic
        fun neighbourCentre(side: Direction): Vector3f = BlockSceneGeometry.unit(side).mul(NEIGHBOUR_DISTANCE)

        /**
         * Where each member of a structure sits in the view: its block's centre relative to the centre of the
         * members' bounding box, in blocks.
         */
        @JvmStatic
        fun memberCentres(members: List<BlockPos>): Map<BlockPos, Vector3f> {
            val minX = members.minOf { it.x }
            val minY = members.minOf { it.y }
            val minZ = members.minOf { it.z }
            val cx = (minX + members.maxOf { it.x }) / 2f
            val cy = (minY + members.maxOf { it.y }) / 2f
            val cz = (minZ + members.maxOf { it.z }) / 2f
            return members.associateWith { Vector3f(it.x - cx, it.y - cy, it.z - cz) }
        }

        /**
         * The view's scale (pixels per block): [SCALE] for one block and its neighbours; for a structure, small enough
         * that its bounding box stays inside the view however it is turned.
         */
        @JvmStatic
        fun fitScale(members: List<BlockPos>): Float {
            if (members.size <= 1) return SCALE
            val dx = (members.maxOf { it.x } - members.minOf { it.x } + 1).toFloat()
            val dy = (members.maxOf { it.y } - members.minOf { it.y } + 1).toFloat()
            val dz = (members.maxOf { it.z } - members.minOf { it.z } + 1).toFloat()
            val diagonal = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            return minOf(SCALE * 1.5f, VIEW * FILL / diagonal)
        }

        @JvmStatic
        fun colorFor(io: EnumAutomaticIO): Int = when (io) {
            EnumAutomaticIO.INPUT -> INPUT
            EnumAutomaticIO.OUTPUT -> OUTPUT
            EnumAutomaticIO.NONE -> 0
        }

        /**
         * Lightens an overlay colour for the hovered face (a faint white for a face without a setting).
         */
        @JvmStatic
        fun highlight(color: Int): Int {
            if (color == 0) return 0x50FFFFFF
            val r = ((color shr 16 and 255) + 255) / 2
            val g = ((color shr 8 and 255) + 255) / 2
            val b = ((color and 255) + 255) / 2
            return (0xA0 shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}

