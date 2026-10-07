package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.research.TechTreeLayout
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/** How a [NodeTreeView] frames a node: done (green), available (yellow) or locked (grey, shaded). */
enum class NodeState { DONE, AVAILABLE, LOCKED }

/**
 * How a node is drawn: its [icon] in a frame for its [state], a bar under it filled to [progress] (0 to 1; drawn while
 * available and above 0), and a [badge] at its top right (e.g. a queue place), if any.
 */
data class NodeLook @JvmOverloads constructor(
    val icon: ItemStack,
    val state: NodeState,
    val progress: Float = 0f,
    val badge: String? = null,
)

/**
 * What a [NodeTreeView] shows: a laid-out graph ([TechTreeLayout.layoutGraph]), each node's look (null hides it and its
 * links) and tooltip. Read every frame, so keep it cheap or cache it.
 */
interface NodeTreeModel {
    val layout: TechTreeLayout.Result

    fun look(id: Identifier): NodeLook?

    fun tooltip(id: Identifier): List<Component>

    companion object {
        @JvmField
        val EMPTY: NodeTreeModel = object : NodeTreeModel {
            override val layout = TechTreeLayout.Result(emptyMap(), emptyList())
            override fun look(id: Identifier): NodeLook? = null
            override fun tooltip(id: Identifier): List<Component> = emptyList()
        }
    }
}

/**
 * A tree of unlockable things, laid out left to right ([TechTreeLayout]): each node as its icon in a frame coloured by
 * its [NodeState], progress under it, a badge, links from what it needs (green once that is done) and a tooltip. Drag
 * to pan, scroll to pan up and down (with shift, sideways); clicking a node calls [onSelect], right-clicking it
 * [onAlternate]. It opens centred on [selected], if any, which is drawn with a white frame. [TechTreeView] shows tech
 * trees with it; mods can show their own trees (skills, talents) through a [NodeTreeModel].
 */
open class NodeTreeView @JvmOverloads constructor(
    width: Int,
    height: Int,
    private val model: () -> NodeTreeModel,
    private val selected: () -> Identifier? = { null },
    private val onSelect: (Identifier) -> Unit = {},
    private val onAlternate: (Identifier) -> Unit = {},
) : ScreenComponent(width, height) {
    private var panX = Double.NaN
    private var panY = Double.NaN
    private var pressed = false
    private var dragged = false

    /** Forgets the pan, so the view opens again on [selected] (e.g. after switching to another tree). */
    fun recentre() {
        panX = Double.NaN
        panY = Double.NaN
    }

    /**
     * Pans so the view opens centred on [selected] if it has one, else at the layout's top left.
     */
    private fun clamp(layout: TechTreeLayout.Result) {
        if (panX.isNaN() || panY.isNaN()) {
            val point = selected()?.let(layout.positions::get)
            panX = point?.let { TechTreeGeometry.PAD + it.x * TechTreeGeometry.CELL_WIDTH + TechTreeGeometry.NODE / 2.0 - width / 2.0 } ?: 0.0
            panY = point?.let { TechTreeGeometry.PAD + it.y * TechTreeGeometry.CELL_HEIGHT + TechTreeGeometry.NODE / 2.0 - height / 2.0 } ?: 0.0
        }
        panX = TechTreeGeometry.clampPan(panX, TechTreeGeometry.contentWidth(layout), width)
        panY = TechTreeGeometry.clampPan(panY, TechTreeGeometry.contentHeight(layout), height)
    }

    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        val model = model()
        val layout = model.layout
        clamp(layout)
        val looks = layout.positions.keys.mapNotNull { id -> model.look(id)?.let { id to it } }.toMap()

        ScreenStyle.frame(graphics, x, y, width, height)
        graphics.fill(x, y, x + width, y + height, BACKGROUND)
        graphics.enableScissor(x, y, x + width, y + height)
        for (edge in layout.edges) {
            val from = looks[edge.from] ?: continue
            if (edge.to !in looks) continue
            drawEdge(graphics, layout, edge, if (from.state == NodeState.DONE) EDGE_DONE else EDGE)
        }
        val chosen = selected()
        for ((id, look) in looks) {
            val point = layout.positions.getValue(id)
            val nx = x + TechTreeGeometry.nodeX(point, panX)
            val ny = y + TechTreeGeometry.nodeY(point, panY)
            val n = TechTreeGeometry.NODE
            if (id == chosen) graphics.fill(nx - 2, ny - 2, nx + n + 2, ny + n + 2, SELECTED)
            graphics.fill(nx, ny, nx + n, ny + n, frameColor(look.state))
            graphics.fill(nx + 1, ny + 1, nx + n - 1, ny + n - 1, NODE_FILL)
            graphics.fakeItem(look.icon, nx + 2, ny + 2)
            if (look.state == NodeState.LOCKED) graphics.fill(nx + 1, ny + 1, nx + n - 1, ny + n - 1, LOCKED_SHADE)
            if (look.state == NodeState.AVAILABLE && look.progress > 0f) {
                graphics.fill(nx, ny + n + 1, nx + n, ny + n + 3, ScreenStyle.DARK)
                graphics.fill(nx, ny + n + 1, nx + (n * look.progress).toInt().coerceIn(0, n), ny + n + 3, ScreenStyle.PROGRESS)
            }
        }
        val badged = looks.filterValues { it.badge != null }
        if (badged.isNotEmpty()) {
            graphics.nextStratum()
            val font = host.hostFont
            for ((id, look) in badged) {
                val point = layout.positions.getValue(id)
                val label = look.badge!!
                val w = font.width(label) + 2
                val bx = x + TechTreeGeometry.nodeX(point, panX) + TechTreeGeometry.NODE - w + 1
                val by = y + TechTreeGeometry.nodeY(point, panY) - 2
                graphics.fill(bx, by, bx + w, by + 9, BADGE)
                graphics.text(font, label, bx + 1, by + 1, BADGE_TEXT, false)
            }
        }
        graphics.disableScissor()

        if (!pressed && contains(mouseX.toDouble(), mouseY.toDouble())) {
            val hovered = TechTreeGeometry.nodeAt(layout, looks.keys, panX, panY, (mouseX - x).toDouble(), (mouseY - y).toDouble())
            if (hovered != null) graphics.setTooltipForNextFrame(host.hostFont, model.tooltip(hovered), java.util.Optional.empty(), mouseX, mouseY)
        }
    }

    /**
     * Right-angled segments from the prerequisite's right side, through each waypoint's centre, to the node's left
     * side; each step turns halfway between columns.
     */
    private fun drawEdge(graphics: GuiGraphicsExtractor, layout: TechTreeLayout.Result, edge: TechTreeLayout.Edge, color: Int) {
        val half = TechTreeGeometry.NODE / 2
        val from = layout.positions[edge.from] ?: return
        val to = layout.positions[edge.to] ?: return
        val points = listOf(from) + edge.waypoints + to
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val ax = x + TechTreeGeometry.nodeX(a, panX) + if (i == 0) TechTreeGeometry.NODE else half
            val ay = y + TechTreeGeometry.nodeY(a, panY) + half
            val bx = x + TechTreeGeometry.nodeX(b, panX) + if (i == points.size - 2) 0 else half
            val by = y + TechTreeGeometry.nodeY(b, panY) + half
            val mid = (ax + bx) / 2
            hLine(graphics, ax, mid, ay, color)
            vLine(graphics, mid, ay, by, color)
            hLine(graphics, mid, bx, by, color)
        }
    }

    private fun hLine(graphics: GuiGraphicsExtractor, x0: Int, x1: Int, y: Int, color: Int) = graphics.fill(minOf(x0, x1), y, maxOf(x0, x1) + 1, y + 1, color)

    private fun vLine(graphics: GuiGraphicsExtractor, x: Int, y0: Int, y1: Int, color: Int) = graphics.fill(x, minOf(y0, y1), x + 1, maxOf(y0, y1) + 1, color)

    private fun nodeAt(mx: Double, my: Double): Identifier? {
        val model = model()
        val visible = model.layout.positions.keys.filterTo(HashSet()) { model.look(it) != null }
        return TechTreeGeometry.nodeAt(model.layout, visible, panX, panY, mx - x, my - y)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (!contains(event.x(), event.y())) return false
        if (event.button() == 1) {
            nodeAt(event.x(), event.y())?.let(onAlternate)
            return true
        }
        if (event.button() != 0) return false
        pressed = true
        dragged = false
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (!pressed) return false
        if (dx != 0.0 || dy != 0.0) dragged = true
        panX -= dx
        panY -= dy
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (!pressed) return false
        pressed = false
        if (dragged) return true
        nodeAt(event.x(), event.y())?.let(onSelect)
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!contains(mouseX, mouseY)) return false
        val sideways = Minecraft.getInstance().hasShiftDown()
        if (sideways) panX -= scrollY * SCROLL else panY -= scrollY * SCROLL
        panX -= scrollX * SCROLL
        return true
    }

    private fun frameColor(state: NodeState) = when (state) {
        NodeState.DONE -> DONE
        NodeState.AVAILABLE -> AVAILABLE
        NodeState.LOCKED -> LOCKED
    }

    companion object {
        const val BACKGROUND = 0xFF1A1A1A.toInt()
        const val NODE_FILL = 0xFF303030.toInt()
        const val DONE = 0xFF55CC55.toInt()
        const val AVAILABLE = 0xFFDDCC44.toInt()
        const val LOCKED = 0xFF5A5A5A.toInt()
        const val LOCKED_SHADE = 0xA0000000.toInt()
        const val SELECTED = 0xFFFFFFFF.toInt()
        const val EDGE = 0xFF555555.toInt()
        const val EDGE_DONE = 0xFF55AA55.toInt()
        const val BADGE = 0xFF1E5A78.toInt()
        const val BADGE_TEXT = 0xFFFFFFFF.toInt()
        const val SCROLL = 16.0
    }
}
