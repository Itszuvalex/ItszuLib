package com.itszuvalex.itszulib.client.screen

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.world.inventory.AbstractContainerMenu
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private class Box(width: Int, height: Int, private val shown: Boolean = true) : ScreenComponent(width, height) {
    var inits = 0
    override val visible: Boolean get() = shown
    override fun init(host: ComponentHost) {
        inits++
    }
    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {}
}

/** Containers never touch the host themselves. */
private object NoHost : ComponentHost {
    override val hostFont: Font get() = error("no font in tests")
    override val hostMenu: AbstractContainerMenu get() = error("no menu in tests")
    override fun <T> addHostWidget(widget: T): T where T : GuiEventListener, T : Renderable, T : NarratableEntry = error("no widgets in tests")
}

private fun ScreenComponent.placeAt(x: Int, y: Int) {
    measure(NoHost)
    this.x = x
    this.y = y
    init(NoHost)
}

class LayoutTests {
    @Test
    fun Align_StartCenterEnd() {
        Assertions.assertEquals(0, LayoutMath.align(10, 30, Align.START))
        Assertions.assertEquals(10, LayoutMath.align(10, 30, Align.CENTER))
        Assertions.assertEquals(20, LayoutMath.align(10, 30, Align.END))
    }

    @Test
    fun Line_OffsetsAndExtent() {
        Assertions.assertEquals(listOf(0, 14, 22), LayoutMath.line(listOf(10, 4, 6), 4))
        Assertions.assertEquals(28, LayoutMath.extent(listOf(10, 4, 6), 4))
        Assertions.assertEquals(0, LayoutMath.extent(emptyList(), 4))
    }

    @Test
    fun Grid_FillsRowsLeftToRight() {
        Assertions.assertEquals(listOf(0 to 0, 20 to 0, 0 to 12, 20 to 12, 0 to 24), LayoutMath.grid(5, 2, 18, 10, 2))
    }

    @Test
    fun Anchored_MarginsMoveInwardsOnEverySide() {
        Assertions.assertEquals(8 to 6, LayoutMath.anchored(Anchor.TOP_LEFT, 0, 0, 176, 166, 20, 10, 8, 6))
        Assertions.assertEquals(176 - 20 - 8 to 166 - 10 - 6, LayoutMath.anchored(Anchor.BOTTOM_RIGHT, 0, 0, 176, 166, 20, 10, 8, 6))
        Assertions.assertEquals(8 + 78 to 17 + 20, LayoutMath.anchored(Anchor.CENTER, 8, 17, 160, 50, 4, 10, 0, 0))
    }

    @Test
    fun Row_PlacesSideBySide_AlignsInTallest_SkipsHidden() {
        val a = Box(10, 20)
        val hidden = Box(50, 50, shown = false)
        val b = Box(6, 10)
        val row = Row(listOf(a, hidden, b), gap = 4, align = Align.CENTER)
        row.placeAt(100, 50)
        Assertions.assertEquals(20, row.width)
        Assertions.assertEquals(20, row.height)
        Assertions.assertEquals(100 to 50, a.x to a.y)
        Assertions.assertEquals(114 to 55, b.x to b.y)
        Assertions.assertEquals(1, a.inits, "children init once")
    }

    @Test
    fun Column_InRow_NestsAndMeasuresBeforePlacement() {
        val top = Box(30, 9)
        val bottom = Box(10, 9)
        val column = Column(listOf(top, bottom), gap = 3, align = Align.END)
        val gauge = Box(8, 52)
        val row = Row(gauge, column)
        row.placeAt(0, 0)
        Assertions.assertEquals(8 + 4 + 30, row.width)
        Assertions.assertEquals(52, row.height)
        Assertions.assertEquals(12 to 0, top.x to top.y)
        Assertions.assertEquals(12 + 20 to 12, bottom.x to bottom.y)
    }

    @Test
    fun Grid_CentresChildrenInEqualCells() {
        val big = Box(18, 18)
        val small = Box(8, 8)
        val grid = Grid(listOf(big, small, small.let { Box(8, 8) }), columns = 2, gap = 2)
        grid.placeAt(10, 10)
        Assertions.assertEquals(38 to 38, grid.width to grid.height)
        Assertions.assertEquals(10 + 20 + 5 to 10 + 5, small.x to small.y)
    }
}
