package com.itszuvalex.itszulib.client.screen

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent

/**
 * Where a component sits along an axis inside the space it is given.
 */
enum class Align { START, CENTER, END }

/**
 * Where a component sits in a rectangle ([ComponentScreen.addComponent] with an anchor).
 */
enum class Anchor(val horizontal: Align, val vertical: Align) {
    TOP_LEFT(Align.START, Align.START),
    TOP(Align.CENTER, Align.START),
    TOP_RIGHT(Align.END, Align.START),
    LEFT(Align.START, Align.CENTER),
    CENTER(Align.CENTER, Align.CENTER),
    RIGHT(Align.END, Align.CENTER),
    BOTTOM_LEFT(Align.START, Align.END),
    BOTTOM(Align.CENTER, Align.END),
    BOTTOM_RIGHT(Align.END, Align.END),
}

/**
 * Layout arithmetic, pure: positions from sizes, for [Row], [Column], [Grid] and anchored placement.
 */
object LayoutMath {
    /** Offset of an item of [size] aligned in [space]. */
    @JvmStatic
    fun align(size: Int, space: Int, align: Align): Int = when (align) {
        Align.START -> 0
        Align.CENTER -> (space - size) / 2
        Align.END -> space - size
    }

    /**
     * Offsets along a line of items of [sizes], [gap] apart: the line's own extent is the sum of sizes and gaps.
     */
    @JvmStatic
    fun line(sizes: List<Int>, gap: Int): List<Int> {
        var at = 0
        return sizes.map { size -> at.also { at += size + gap } }
    }

    /** The extent of a line of [sizes], [gap] apart. */
    @JvmStatic
    fun extent(sizes: List<Int>, gap: Int): Int = if (sizes.isEmpty()) 0 else sizes.sum() + gap * (sizes.size - 1)

    /**
     * Cell origins of [count] cells of [cellWidth] x [cellHeight] in rows of [columns], [gap] apart, filled left to
     * right then top to bottom.
     */
    @JvmStatic
    fun grid(count: Int, columns: Int, cellWidth: Int, cellHeight: Int, gap: Int): List<Pair<Int, Int>> {
        require(columns > 0) { "A grid needs at least one column" }
        return (0 until count).map { i -> (i % columns) * (cellWidth + gap) to (i / columns) * (cellHeight + gap) }
    }

    /**
     * Where a [width] x [height] item anchored at [anchor] inside the rectangle at ([x], [y]) of [areaWidth] x
     * [areaHeight] goes, moved by ([dx], [dy]) (positive moves inwards from the anchored edges, so margins keep their
     * meaning on every side).
     */
    @JvmStatic
    fun anchored(anchor: Anchor, x: Int, y: Int, areaWidth: Int, areaHeight: Int, width: Int, height: Int, dx: Int, dy: Int): Pair<Int, Int> {
        fun inward(align: Align, d: Int) = when (align) {
            Align.START -> d
            Align.CENTER -> d
            Align.END -> -d
        }
        return x + align(width, areaWidth, anchor.horizontal) + inward(anchor.horizontal, dx) to
            y + align(height, areaHeight, anchor.vertical) + inward(anchor.vertical, dy)
    }
}

/**
 * A component holding others, which it lays out ([layout]) whenever the screen inits, and to which it passes drawing
 * and input. Invisible children take no space. Its size follows its children's.
 */
abstract class ContainerComponent(children: List<ScreenComponent>) : ScreenComponent(0, 0) {
    val children: List<ScreenComponent> = children.toList()

    protected fun shown(): List<ScreenComponent> = children.filter { it.visible }

    /** Sets the children's positions (from this container's [x], [y]) and this container's size. */
    protected abstract fun layout()

    /** Children size themselves (a label measures its text), then this container works out its own size. */
    override fun measure(host: ComponentHost) {
        for (child in children) child.measure(host)
        layout()
    }

    /** Children take their places from this container's position, then init (adding their widgets once). */
    override fun init(host: ComponentHost) {
        layout()
        for (child in children) child.init(host)
    }

    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        for (child in shown()) child.extract(graphics, mouseX, mouseY, host)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean = shown().any { it.mouseClicked(event, doubleClick) }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean = shown().any { it.mouseDragged(event, dx, dy) }

    override fun mouseReleased(event: MouseButtonEvent): Boolean = shown().any { it.mouseReleased(event) }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
        shown().any { it.mouseScrolled(mouseX, mouseY, scrollX, scrollY) }
}

/**
 * Children side by side, left to right, [gap] apart, aligned vertically by [align] within the tallest.
 */
class Row @JvmOverloads constructor(
    children: List<ScreenComponent>,
    private val gap: Int = 4,
    private val align: Align = Align.START,
) : ContainerComponent(children) {
    constructor(vararg children: ScreenComponent) : this(children.toList())

    override fun layout() {
        val items = shown()
        val offsets = LayoutMath.line(items.map { it.width }, gap)
        width = LayoutMath.extent(items.map { it.width }, gap)
        height = items.maxOfOrNull { it.height } ?: 0
        items.forEachIndexed { i, c ->
            c.x = x + offsets[i]
            c.y = y + LayoutMath.align(c.height, height, align)
        }
    }
}

/**
 * Children one under another, top to bottom, [gap] apart, aligned horizontally by [align] within the widest.
 */
class Column @JvmOverloads constructor(
    children: List<ScreenComponent>,
    private val gap: Int = 2,
    private val align: Align = Align.START,
) : ContainerComponent(children) {
    constructor(vararg children: ScreenComponent) : this(children.toList())

    override fun layout() {
        val items = shown()
        val offsets = LayoutMath.line(items.map { it.height }, gap)
        height = LayoutMath.extent(items.map { it.height }, gap)
        width = items.maxOfOrNull { it.width } ?: 0
        items.forEachIndexed { i, c ->
            c.x = x + LayoutMath.align(c.width, width, align)
            c.y = y + offsets[i]
        }
    }
}

/**
 * Children in rows of [columns] equal cells (as large as the largest child), [gap] apart, each centred in its cell.
 */
class Grid @JvmOverloads constructor(
    children: List<ScreenComponent>,
    private val columns: Int,
    private val gap: Int = 2,
) : ContainerComponent(children) {
    override fun layout() {
        val items = shown()
        val cellW = items.maxOfOrNull { it.width } ?: 0
        val cellH = items.maxOfOrNull { it.height } ?: 0
        val cells = LayoutMath.grid(items.size, columns, cellW, cellH, gap)
        val cols = minOf(columns, items.size)
        val rows = if (items.isEmpty()) 0 else (items.size + columns - 1) / columns
        width = LayoutMath.extent(List(cols) { cellW }, gap)
        height = LayoutMath.extent(List(rows) { cellH }, gap)
        items.forEachIndexed { i, c ->
            c.x = x + cells[i].first + LayoutMath.align(c.width, cellW, Align.CENTER)
            c.y = y + cells[i].second + LayoutMath.align(c.height, cellH, Align.CENTER)
        }
    }
}

/**
 * Empty space in a [Row] or [Column].
 */
class Spacer(width: Int, height: Int) : ScreenComponent(width, height) {
    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {}
}
