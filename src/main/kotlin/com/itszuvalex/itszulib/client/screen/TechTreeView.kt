package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.client.ScreenMath
import com.itszuvalex.itszulib.research.TechTree
import com.itszuvalex.itszulib.research.TechTreeLayout
import com.itszuvalex.itszulib.research.Technologies
import com.itszuvalex.itszulib.research.TechnologyState
import com.itszuvalex.itszulib.team.Research
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

/**
 * Where a [TechTreeView] draws a layout: cells of [CELL_WIDTH] x [CELL_HEIGHT] pixels, each node a [NODE] pixel
 * square at its cell's top left, the whole layout inset by [PAD] and shifted by the pan. Pure, for tests.
 */
object TechTreeGeometry {
    const val CELL_WIDTH = 40
    const val CELL_HEIGHT = 28
    const val NODE = 20
    const val PAD = 8

    /** The node's top left, relative to the view's top left. */
    @JvmStatic
    fun nodeX(point: TechTreeLayout.Point, panX: Double): Int = (PAD + point.x * CELL_WIDTH - panX).roundToInt()

    @JvmStatic
    fun nodeY(point: TechTreeLayout.Point, panY: Double): Int = (PAD + point.y * CELL_HEIGHT - panY).roundToInt()

    /** The layout's size in pixels, padding included. */
    @JvmStatic
    fun contentWidth(layout: TechTreeLayout.Result): Int = if (layout.positions.isEmpty()) 0 else ((layout.width - 1) * CELL_WIDTH).roundToInt() + NODE + 2 * PAD

    @JvmStatic
    fun contentHeight(layout: TechTreeLayout.Result): Int = if (layout.positions.isEmpty()) 0 else ((layout.height - 1) * CELL_HEIGHT).roundToInt() + NODE + 2 * PAD

    /**
     * The pan that keeps [content] pixels in a [view] pixel window: centred if it fits, else clamped to its ends.
     */
    @JvmStatic
    fun clampPan(pan: Double, content: Int, view: Int): Double = if (content <= view) (content - view) / 2.0 else pan.coerceIn(0.0, (content - view).toDouble())

    /**
     * The technology whose node is at ([mx], [my]) relative to the view's top left, among [visible].
     */
    @JvmStatic
    fun nodeAt(layout: TechTreeLayout.Result, visible: Set<Identifier>, panX: Double, panY: Double, mx: Double, my: Double): Identifier? =
        layout.positions.entries.firstOrNull { (id, p) ->
            id in visible && mx >= nodeX(p, panX) && mx < nodeX(p, panX) + NODE && my >= nodeY(p, panY) && my < nodeY(p, panY) + NODE
        }?.key
}

/**
 * Shows one tech tree ([TechTree]) laid out by [TechTreeLayout]: each technology as its icon in a frame coloured by
 * its state for the team (green researched, yellow available, grey locked; hidden ones are not drawn), progress under
 * it, its place in the team's research queue ([Research.queue]) as a badge, links to its prerequisites, and a tooltip
 * with its name, description, progress, other requirements (resources, items to hand in), rewards, queue place and
 * missing prerequisites. Drag to pan, scroll to pan up and down
 * (with shift, sideways); clicking a technology calls [onSelect], right-clicking it [onAlternate]. It opens centred on
 * [selected], if any.
 *
 * @param research The team's research; by default the local player's (synced team data).
 * @param selected Drawn with a white frame, e.g. what a machine is researching.
 */
class TechTreeView @JvmOverloads constructor(
    width: Int,
    height: Int,
    private val tree: Identifier,
    private val research: () -> Research = { Minecraft.getInstance().player?.let(TechTree::research) ?: Research.EMPTY },
    private val selected: () -> Identifier? = { null },
    private val onSelect: (Identifier) -> Unit = {},
    private val onAlternate: (Identifier) -> Unit = {},
) : ScreenComponent(width, height) {
    private var panX = Double.NaN
    private var panY = Double.NaN
    private var pressed = false
    private var dragged = false

    private fun technologies(): Technologies = Minecraft.getInstance().level?.registryAccess()?.let(TechTree::of) ?: Technologies.EMPTY

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
        val techs = technologies()
        val layout = techs.layout(tree)
        val research = research()
        clamp(layout)
        val states = layout.positions.keys.associateWith { techs.state(it, research) ?: TechnologyState.HIDDEN }
        val visible = states.filterValues { it != TechnologyState.HIDDEN }.keys

        ScreenStyle.frame(graphics, x, y, width, height)
        graphics.fill(x, y, x + width, y + height, BACKGROUND)
        graphics.enableScissor(x, y, x + width, y + height)
        for (edge in layout.edges) {
            if (edge.from !in visible || edge.to !in visible) continue
            val color = if (states[edge.from] == TechnologyState.RESEARCHED) EDGE_DONE else EDGE
            drawEdge(graphics, layout, edge, color)
        }
        val chosen = selected()
        for ((id, point) in layout.positions) {
            val state = states.getValue(id)
            if (state == TechnologyState.HIDDEN) continue
            val tech = techs[id] ?: continue
            val nx = x + TechTreeGeometry.nodeX(point, panX)
            val ny = y + TechTreeGeometry.nodeY(point, panY)
            val n = TechTreeGeometry.NODE
            if (id == chosen) graphics.fill(nx - 2, ny - 2, nx + n + 2, ny + n + 2, SELECTED)
            graphics.fill(nx, ny, nx + n, ny + n, frameColor(state))
            graphics.fill(nx + 1, ny + 1, nx + n - 1, ny + n - 1, NODE_FILL)
            graphics.fakeItem(tech.iconStack(), nx + 2, ny + 2)
            if (state == TechnologyState.LOCKED) graphics.fill(nx + 1, ny + 1, nx + n - 1, ny + n - 1, LOCKED_SHADE)
            val progress = research.progressOf(id)
            if (state == TechnologyState.AVAILABLE && progress > 0 && tech.cost > 0) {
                graphics.fill(nx, ny + n + 1, nx + n, ny + n + 3, ScreenStyle.DARK)
                graphics.fill(nx, ny + n + 1, nx + (n * progress / tech.cost).toInt().coerceIn(0, n), ny + n + 3, ScreenStyle.PROGRESS)
            }
        }
        val queued = research.queue.withIndex().filter { (_, id) -> id in visible && layout.positions.containsKey(id) }
        if (queued.isNotEmpty()) {
            graphics.nextStratum()
            val font = host.hostFont
            for ((index, id) in queued) {
                val point = layout.positions.getValue(id)
                val label = (index + 1).toString()
                val w = font.width(label) + 2
                val bx = x + TechTreeGeometry.nodeX(point, panX) + TechTreeGeometry.NODE - w + 1
                val by = y + TechTreeGeometry.nodeY(point, panY) - 2
                graphics.fill(bx, by, bx + w, by + 9, QUEUE_BADGE)
                graphics.text(font, label, bx + 1, by + 1, QUEUE_TEXT, false)
            }
        }
        graphics.disableScissor()

        if (!pressed && contains(mouseX.toDouble(), mouseY.toDouble())) {
            val hovered = TechTreeGeometry.nodeAt(layout, visible, panX, panY, (mouseX - x).toDouble(), (mouseY - y).toDouble())
            if (hovered != null) graphics.setTooltipForNextFrame(host.hostFont, tooltip(techs, hovered, states.getValue(hovered), research), java.util.Optional.empty(), mouseX, mouseY)
        }
    }

    /**
     * Right-angled segments from the prerequisite's right side, through each waypoint's centre, to the technology's
     * left side; each step turns halfway between columns.
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

    private fun tooltip(techs: Technologies, id: Identifier, state: TechnologyState, research: Research): List<Component> {
        val tech = techs[id] ?: return emptyList()
        val lines = ArrayList<Component>()
        lines += tech.displayName(id).copy().withStyle(
            when (state) {
                TechnologyState.RESEARCHED -> ChatFormatting.GREEN
                TechnologyState.AVAILABLE -> ChatFormatting.YELLOW
                else -> ChatFormatting.GRAY
            },
        )
        lines += tech.displayDescription(id).copy().withStyle(ChatFormatting.GRAY)
        when (state) {
            TechnologyState.RESEARCHED -> lines += Component.translatable("gui.itszulib.research.researched").withStyle(ChatFormatting.GREEN)
            TechnologyState.AVAILABLE -> lines += Component.translatable(
                "gui.itszulib.research.progress",
                ScreenMath.formatAmount(research.progressOf(id)),
                ScreenMath.formatAmount(tech.cost),
            ).withStyle(ChatFormatting.YELLOW)
            else -> {}
        }
        if (state != TechnologyState.RESEARCHED) {
            for ((resource, amount) in tech.resources) {
                val have = amount - (techs.remaining(id, research)?.resources?.get(resource) ?: amount)
                lines += Component.translatable("gui.itszulib.research.resource", Technologies.resourceName(resource), ScreenMath.formatAmount(have), ScreenMath.formatAmount(amount))
                    .withStyle(if (have >= amount) ChatFormatting.GREEN else ChatFormatting.YELLOW)
            }
            techs.remaining(id, research)?.items?.forEach { (item, left) ->
                val name = item.ingredient().items().findFirst().map { net.minecraft.world.item.ItemStack(it).hoverName }.orElse(Component.literal("?"))
                lines += Component.translatable("gui.itszulib.research.item", name, item.count() - left, item.count())
                    .withStyle(if (left <= 0) ChatFormatting.GREEN else ChatFormatting.YELLOW)
            }
        }
        if (tech.rewards.isNotEmpty()) {
            lines += Component.translatable("gui.itszulib.research.rewards").withStyle(ChatFormatting.LIGHT_PURPLE)
            for (reward in tech.rewards) lines += Component.literal("  ${reward.count()} × ").append(reward.create().hoverName).withStyle(ChatFormatting.LIGHT_PURPLE)
        }
        research.queuePosition(id).takeIf { it >= 0 }?.let {
            lines += Component.translatable("gui.itszulib.research.queued", it + 1).withStyle(ChatFormatting.AQUA)
        }
        if (state == TechnologyState.LOCKED || state == TechnologyState.HIDDEN) {
            lines += Component.translatable("gui.itszulib.research.requires").withStyle(ChatFormatting.RED)
            for (p in techs.missingPrerequisites(id, research)) {
                lines += Component.literal("  ").append(techs[p]?.displayName(p) ?: Component.literal(p.toString())).withStyle(ChatFormatting.RED)
            }
        }
        return lines
    }

    private fun nodeAt(mx: Double, my: Double): Identifier? {
        val techs = technologies()
        val layout = techs.layout(tree)
        val research = research()
        val visible = layout.positions.keys.filterTo(HashSet()) { (techs.state(it, research) ?: TechnologyState.HIDDEN) != TechnologyState.HIDDEN }
        return TechTreeGeometry.nodeAt(layout, visible, panX, panY, mx - x, my - y)
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

    private fun frameColor(state: TechnologyState) = when (state) {
        TechnologyState.RESEARCHED -> RESEARCHED
        TechnologyState.AVAILABLE -> AVAILABLE
        else -> LOCKED
    }

    companion object {
        const val BACKGROUND = 0xFF1A1A1A.toInt()
        const val NODE_FILL = 0xFF303030.toInt()
        const val RESEARCHED = 0xFF55CC55.toInt()
        const val AVAILABLE = 0xFFDDCC44.toInt()
        const val LOCKED = 0xFF5A5A5A.toInt()
        const val LOCKED_SHADE = 0xA0000000.toInt()
        const val SELECTED = 0xFFFFFFFF.toInt()
        const val EDGE = 0xFF555555.toInt()
        const val EDGE_DONE = 0xFF55AA55.toInt()
        const val QUEUE_BADGE = 0xFF1E5A78.toInt()
        const val QUEUE_TEXT = 0xFFFFFFFF.toInt()
        const val SCROLL = 16.0
    }
}
