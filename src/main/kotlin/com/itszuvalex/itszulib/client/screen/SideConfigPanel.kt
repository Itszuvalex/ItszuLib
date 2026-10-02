package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.client.scene.BlockSceneBlock
import com.itszuvalex.itszulib.client.scene.BlockSceneGeometry
import com.itszuvalex.itszulib.client.scene.BlockSceneOverlay
import com.itszuvalex.itszulib.client.scene.BlockScenePickBox
import com.itszuvalex.itszulib.client.scene.BlockSceneView
import com.itszuvalex.itszulib.core.EnumAutomaticIO
import com.itszuvalex.itszulib.menu.MenuActionPayload
import com.itszuvalex.itszulib.menu.MenuCore
import com.itszuvalex.itszulib.menu.MenuSideConfig
import com.itszuvalex.itszulib.menu.SideConfigMode
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.multiplayer.ClientLevel
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
     * The machine face under the mouse: the face itself, or the side of the neighbour hit.
     */
    private fun faceAt(mx: Double, my: Double): Direction? {
        val hit = view.pick(mx, my, boxes(neighbours())) ?: return null
        return hit.key ?: hit.face
    }

    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        ScreenStyle.panel(graphics, x, y, width, height)
        graphics.fill(view.x, view.y, view.x + VIEW, view.y + VIEW, if (ScreenThemeConfig.sideConfigLight()) LIGHT_BACKGROUND else ScreenStyle.DARK)
        val be = side.blockEntity
        val level = be.level as? ClientLevel ?: return
        val config = side.configuration(mode)
        val hovered = if (view.dragging) null else faceAt(mouseX.toDouble(), mouseY.toDouble())
        val neighbours = neighbours()
        val blocks = listOf(BlockSceneBlock.of(level, be.blockPos, Vector3f(), 1f, be.blockState)) +
            neighbours.map { BlockSceneBlock.of(level, be.blockPos.relative(it), neighbourCentre(it), NEIGHBOUR_SIZE) }
        val overlays = Direction.entries.mapNotNull { face ->
            val base = config?.let { colorFor(it.getIOForAbsoluteFacing(face)) } ?: 0
            val color = if (face == hovered) highlight(base) else base
            if (color == 0) null else BlockSceneOverlay(Vector3f(), OVERLAY_SIZE, face, color)
        }
        view.submit(graphics, blocks, overlays)
        if (hovered != null && config != null) {
            graphics.setTooltipForNextFrame(host.hostFont, listOf(
                Component.translatable("gui.itszulib.side_config.face", Component.translatable("gui.itszulib.side_config.dir.${hovered.serializedName}")),
                Component.translatable("gui.itszulib.side_config.io.${config.getIOForAbsoluteFacing(hovered).name.lowercase()}"),
                Component.translatable("gui.itszulib.side_config.storage", config.getStorageNameForAbsoluteFacing(hovered)),
            ), java.util.Optional.empty(), mouseX, mouseY)
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean = view.press(event.x(), event.y())

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean = view.drag(event.x(), event.y(), dx, dy)

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val click = view.release() ?: return false
        if (!click) return true
        val face = faceAt(event.x(), event.y()) ?: return true
        if (side.configuration(mode) == null) return true
        val backward = Minecraft.getInstance().hasShiftDown()
        ClientPacketDistributor.sendToServer(MenuActionPayload(containerId, MenuCore.ACTION_SIDE_CONFIG, MenuSideConfig.data(face, modeIndex, backward)))
        return true
    }

    companion object {
        const val VIEW = 112
        const val PAD = 4
        const val BUTTON_H = 14
        const val SCALE = 30f

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

