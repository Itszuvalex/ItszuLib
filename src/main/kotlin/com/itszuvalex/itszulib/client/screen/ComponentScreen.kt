package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.menu.MenuCore
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu

/**
 * What a [ScreenComponent] can reach in its screen.
 */
interface ComponentHost {
    val hostFont: Font

    val hostMenu: AbstractContainerMenu

    /**
     * Adds a vanilla widget for as long as the component is shown (widgets are rebuilt when panels open or close).
     */
    fun <T> addHostWidget(widget: T): T where T : GuiEventListener, T : Renderable, T : NarratableEntry
}

/**
 * A piece of a [ComponentScreen]: draws itself in its area ([x], [y] in screen coordinates, set by the screen before
 * [init]), and may take mouse input and add vanilla widgets. Components are created once per screen and keep their
 * state when the screen re-inits (a resize, a panel opening).
 */
abstract class ScreenComponent(var width: Int, var height: Int) {
    var x = 0
    var y = 0

    /**
     * False to skip drawing and input (e.g. a gauge for a storage the block does not have).
     */
    open val visible: Boolean get() = true

    /**
     * Called each time the screen inits, after [x]/[y] are set; add widgets here.
     */
    open fun init(host: ComponentHost) {}

    /**
     * Draws the component (in the screen's background pass, so it may set tooltips and submit 3D elements).
     */
    abstract fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost)

    open fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean = false

    open fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean = false

    open fun mouseReleased(event: MouseButtonEvent): Boolean = false

    open fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean = false

    fun contains(mx: Double, my: Double) = mx >= x && mx < x + width && my >= y && my < y + height
}

/**
 * A component shown beside the screen while its tab is selected. [tab] is the tab button's short label and [title]
 * its tooltip.
 */
class SidePanel(@JvmField val tab: Component, @JvmField val title: Component, @JvmField val component: ScreenComponent)

/**
 * A container screen built from [ScreenComponent]s: components placed in the screen's image ([addComponent]) and side
 * panels ([addPanel]) toggled by tab buttons along the image's right edge, one open at a time. Menus with side
 * configuration ([MenuCore.enableSideConfig]) get a side configuration panel by default ([defaultPanels]).
 *
 * The background is [ScreenStyle]'s plain panel with slot outlines; override [extractPanel] to draw your own.
 */
abstract class ComponentScreen<M : AbstractContainerMenu> @JvmOverloads constructor(
    menu: M,
    inventory: Inventory,
    title: Component,
    width: Int = 176,
    height: Int = 166,
) : AbstractContainerScreen<M>(menu, inventory, title, width, height), ComponentHost {
    private class Placed(val component: ScreenComponent, val x: Int, val y: Int)

    private val placed = ArrayList<Placed>()
    private val panels = ArrayList<SidePanel>()
    private var built = false

    /**
     * The open side panel, or null.
     */
    var openPanel: SidePanel? = null
        private set

    override val hostFont: Font get() = font

    override val hostMenu: AbstractContainerMenu get() = menu

    override fun <T> addHostWidget(widget: T): T where T : GuiEventListener, T : Renderable, T : NarratableEntry = addRenderableWidget(widget)

    /**
     * Adds this screen's components and panels; called once, at the start of the first [init] (before the screen's
     * position is known: components are placed relative to the image).
     */
    protected open fun addComponents() {}

    /**
     * Panels every screen of this kind gets, after [addComponents]: a side configuration panel when the menu has side
     * configuration.
     */
    protected open fun defaultPanels(): List<SidePanel> {
        val side = (menu as? MenuCore)?.sideConfig ?: return emptyList()
        return listOf(SidePanel(Component.translatable("gui.itszulib.side_config.tab"), Component.translatable("gui.itszulib.side_config.title"), SideConfigPanel(side, menu.containerId)))
    }

    /**
     * Places [component] at ([x], [y]) in the screen's image.
     */
    fun <C : ScreenComponent> addComponent(component: C, x: Int, y: Int): C {
        placed += Placed(component, x, y)
        return component
    }

    fun addPanel(panel: SidePanel): SidePanel {
        panels += panel
        return panel
    }

    fun panels(): List<SidePanel> = panels

    /**
     * Opens [panel], or closes it if it is open (null closes any).
     */
    fun togglePanel(panel: SidePanel?) {
        openPanel = if (panel == null || openPanel === panel) null else panel
        rebuildWidgets()
    }

    override fun init() {
        if (!built) {
            built = true
            addComponents()
            defaultPanels().forEach(::addPanel)
        }
        super.init()
        // Centre the image, the tabs and the open panel together, so the panel stays on screen.
        if (panels.isNotEmpty()) {
            val extra = TAB_SIZE + TAB_GAP + (openPanel?.component?.width ?: 0)
            leftPos = maxOf(0, (width - imageWidth - extra) / 2)
        }
        for (p in placed) {
            p.component.x = leftPos + p.x
            p.component.y = topPos + p.y
            p.component.init(this)
        }
        panels.forEachIndexed { i, panel ->
            addRenderableWidget(
                Button.builder(panel.tab) { togglePanel(panel) }
                    .bounds(leftPos + imageWidth, topPos + TAB_GAP + i * (TAB_SIZE + TAB_GAP), TAB_SIZE, TAB_SIZE)
                    .tooltip(Tooltip.create(panel.title)).build(),
            )
        }
        openPanel?.component?.let {
            it.x = leftPos + imageWidth + TAB_SIZE + TAB_GAP
            it.y = topPos
            it.init(this)
        }
    }

    /**
     * The components that draw and take input now: the visible placed ones and the open panel's.
     */
    fun activeComponents(): List<ScreenComponent> =
        placed.map { it.component }.filter { it.visible } + listOfNotNull(openPanel?.component?.takeIf { it.visible })

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractBackground(graphics, mouseX, mouseY, a)
        extractPanel(graphics, mouseX, mouseY)
        for (component in activeComponents()) component.extract(graphics, mouseX, mouseY, this)
    }

    /**
     * Draws the screen's own background under the components.
     */
    protected open fun extractPanel(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        ScreenStyle.panel(graphics, leftPos, topPos, imageWidth, imageHeight)
        for (slot in menu.slots) ScreenStyle.slot(graphics, leftPos + slot.x, topPos + slot.y)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean =
        activeComponents().any { it.mouseClicked(event, doubleClick) } || super.mouseClicked(event, doubleClick)

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean =
        activeComponents().any { it.mouseDragged(event, dx, dy) } || super.mouseDragged(event, dx, dy)

    override fun mouseReleased(event: MouseButtonEvent): Boolean =
        activeComponents().any { it.mouseReleased(event) } || super.mouseReleased(event)

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean =
        activeComponents().any { it.mouseScrolled(x, y, scrollX, scrollY) } || super.mouseScrolled(x, y, scrollX, scrollY)

    /**
     * Areas this screen draws outside its image, in screen coordinates: the tab column and the open side panel.
     * Recipe viewers keep their overlays out of them (ItszuLib's JEI plugin reports them), so panels stay clickable.
     */
    fun extraAreas(): List<Rect2i> {
        if (panels.isEmpty()) return emptyList()
        val areas = ArrayList<Rect2i>()
        areas += Rect2i(leftPos + imageWidth, topPos, TAB_SIZE + TAB_GAP, panels.size * (TAB_SIZE + TAB_GAP) + TAB_GAP)
        openPanel?.component?.let { areas += Rect2i(it.x, it.y, it.width, it.height) }
        return areas
    }

    /**
     * Clicks on the open panel or the tabs are not "outside" (which would drop the carried stack).
     */
    override fun hasClickedOutside(mx: Double, my: Double, xo: Int, yo: Int): Boolean {
        if (openPanel?.component?.contains(mx, my) == true) return false
        val tabs = mx >= leftPos + imageWidth && mx < leftPos + imageWidth + TAB_SIZE && my >= topPos && my < topPos + panels.size * (TAB_SIZE + TAB_GAP) + TAB_GAP
        return !tabs && super.hasClickedOutside(mx, my, xo, yo)
    }

    companion object {
        const val TAB_SIZE = 20
        const val TAB_GAP = 2
    }
}
