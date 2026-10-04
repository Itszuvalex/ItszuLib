package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.menu.MenuCore
import com.itszuvalex.itszulib.menu.SlotLook
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

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
     * Called each time the screen inits, before it is placed: set [width]/[height] if they depend on the screen (a
     * label measuring its text). Containers measure their children here.
     */
    open fun measure(host: ComponentHost) {}

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
 * its tooltip; a non-empty [icon] is drawn on the tab instead of the label, and [accent] tints it ([ButtonAccents]).
 */
class SidePanel @JvmOverloads constructor(
    @JvmField val tab: Component,
    @JvmField val title: Component,
    @JvmField val component: ScreenComponent,
    @JvmField val icon: ItemStack = ItemStack.EMPTY,
    @JvmField val accent: String? = null,
)

/**
 * A container screen built from [ScreenComponent]s: components placed in the screen's image ([addComponent]) and side
 * panels ([addPanel]) toggled by tab buttons ([ThemedButton]s) along the image's right edge, one open at a time. Menus with side
 * configuration ([MenuCore.enableSideConfig]) get a side configuration panel by default ([defaultPanels]).
 *
 * It draws in a [ScreenTheme]: the player's chosen one ([ScreenThemeConfig]) or the screen's [defaultTheme]. The
 * background is the theme's bevelled panel with every slot inset, take-only slots ringed and empty slots' hints faded
 * in ([SlotLook]); override [extractPanel] to draw your own (hand-drawn art can still call [extractSlots]).
 */
abstract class ComponentScreen<M : AbstractContainerMenu> @JvmOverloads constructor(
    menu: M,
    inventory: Inventory,
    title: Component,
    width: Int = 176,
    height: Int = 166,
) : AbstractContainerScreen<M>(menu, inventory, title, width, height), ComponentHost {
    /**
     * A placed component: at ([x], [y]) in the image, or, with an [anchor], anchored in the image or its content area
     * ([inContent]) and moved inwards by ([x], [y]).
     */
    private class Placed(val component: ScreenComponent, val x: Int, val y: Int, val anchor: Anchor? = null, val inContent: Boolean = false)

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
        return listOf(SidePanel(Component.translatable("gui.itszulib.side_config.tab"), Component.translatable("gui.itszulib.side_config.title"), SideConfigPanel(side, menu.containerId), accent = ButtonAccents.IO))
    }

    /**
     * Places [component] at ([x], [y]) in the screen's image.
     */
    fun <C : ScreenComponent> addComponent(component: C, x: Int, y: Int): C {
        placed += Placed(component, x, y)
        return component
    }

    /**
     * Places [component] at [anchor] in the image (or, with [inContent], in [contentArea]), moved inwards from the
     * anchored edges by ([dx], [dy]). Its size is measured each time the screen inits, so anchoring follows it.
     */
    @JvmOverloads
    fun <C : ScreenComponent> addComponent(component: C, anchor: Anchor, dx: Int = 0, dy: Int = 0, inContent: Boolean = false): C {
        placed += Placed(component, dx, dy, anchor, inContent)
        return component
    }

    /**
     * The part of the image for the machine's own content, in image coordinates: inside the 8 pixel border, below the
     * title and above the player inventory's label (the whole inner image when the inventory label is hidden below
     * it).
     */
    fun contentArea(): net.minecraft.client.renderer.Rect2i {
        val top = titleLabelY + font.lineHeight + 2
        val bottom = if (inventoryLabelY in (top + 1) until imageHeight) inventoryLabelY - 2 else imageHeight - 8
        return net.minecraft.client.renderer.Rect2i(8, top, imageWidth - 16, bottom - top)
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
        val content = contentArea()
        for (p in placed) {
            p.component.measure(this)
            val anchor = p.anchor
            if (anchor == null) {
                p.component.x = leftPos + p.x
                p.component.y = topPos + p.y
            } else {
                val (ax, ay, aw, ah) = if (p.inContent) listOf(content.x, content.y, content.width, content.height) else listOf(0, 0, imageWidth, imageHeight)
                val (x, y) = LayoutMath.anchored(anchor, ax, ay, aw, ah, p.component.width, p.component.height, p.x, p.y)
                p.component.x = leftPos + x
                p.component.y = topPos + y
            }
            p.component.init(this)
        }
        panels.forEachIndexed { i, panel ->
            addRenderableWidget(
                ThemedButton(
                    leftPos + imageWidth, topPos + TAB_GAP + i * (TAB_SIZE + TAB_GAP), TAB_SIZE, TAB_SIZE, panel.tab,
                    { togglePanel(panel) }, panel.title, panel.icon, selected = { openPanel === panel }, accent = panel.accent,
                ),
            )
        }
        openPanel?.component?.let {
            it.measure(this)
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

    /**
     * The theme this screen draws with unless the player chose one: a screen kind's look.
     */
    protected open fun defaultTheme(): Identifier = ScreenThemes.LIGHT

    /** The theme this screen draws with now. */
    val theme: ScreenTheme get() = ScreenThemes.resolve(defaultTheme())

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        ScreenStyle.theme = theme
        super.extractBackground(graphics, mouseX, mouseY, a)
        extractPanel(graphics, mouseX, mouseY)
        for (component in activeComponents()) component.extract(graphics, mouseX, mouseY, this)
    }

    /**
     * Draws the screen's own background under the components.
     */
    protected open fun extractPanel(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        ScreenStyle.panel(graphics, leftPos, topPos, imageWidth, imageHeight)
        extractSlots(graphics)
    }

    /**
     * Every slot's inset, take-only slots' rings (first, so neighbouring outputs share an outline), and the faded
     * hints of empty slots.
     */
    protected fun extractSlots(graphics: GuiGraphicsExtractor) {
        for (slot in menu.slots) if (slot.isActive && (slot as? SlotLook)?.isOutput == true) ScreenStyle.outputRing(graphics, leftPos + slot.x, topPos + slot.y)
        // A met requirement shows just its item, without the slot's inset.
        for (slot in menu.slots) if (slot.isActive && !requirementMet(slot)) ScreenStyle.slot(graphics, leftPos + slot.x, topPos + slot.y)
        val hinted = menu.slots.filter { it.isActive && !it.hasItem() && (it as? SlotLook)?.hint()?.isEmpty == false }
        if (hinted.isEmpty()) return
        for (slot in hinted) graphics.fakeItem((slot as SlotLook).hint(), leftPos + slot.x, topPos + slot.y)
        // Over the hints (items draw above fills within a stratum): the slot's face, mostly opaque, fades them.
        graphics.nextStratum()
        val fade = (theme.slot and 0xFFFFFF) or (HINT_FADE shl 24)
        for (slot in hinted) graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, fade)
        // An empty requirement's count, in red (filled ones show theirs in place of the stack size, renderSlotContents).
        for (slot in hinted) {
            val need = (slot as SlotLook).required()
            if (need.isEmpty) continue
            val text = requirementText(0, need.count)
            graphics.text(font, text, leftPos + slot.x + 17 - font.width(text), topPos + slot.y + 9, -1, true)
        }
    }

    /** Hovering an empty requirement slot shows what it wants: the item's tooltip and how many. */
    override fun extractTooltip(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val slot = hoveredSlot
        val need = (slot as? SlotLook)?.required()
        if (slot != null && !slot.hasItem() && need != null && !need.isEmpty && menu.carried.isEmpty) {
            val lines = ArrayList(getTooltipFromItem(minecraft, need))
            lines += Component.translatable("gui.itszulib.requirement.needs", need.count).withStyle(ChatFormatting.RED)
            graphics.setTooltipForNextFrame(font, lines, need.tooltipImage, need, mouseX, mouseY)
            return
        }
        super.extractTooltip(graphics, mouseX, mouseY)
    }

    private fun requirementMet(slot: Slot): Boolean {
        val need = (slot as? SlotLook)?.required() ?: return false
        return !need.isEmpty && slot.item.count >= need.count
    }

    /** A requirement slot's item with how many are still needed (red) for its count, or the full amount (green) once met. */
    override fun renderSlotContents(graphics: GuiGraphicsExtractor, itemStack: ItemStack, slot: Slot, itemCount: String?) {
        val need = (slot as? SlotLook)?.required()
        if (need == null || need.isEmpty || itemStack.isEmpty || itemCount != null) return super.renderSlotContents(graphics, itemStack, slot, itemCount)
        super.renderSlotContents(graphics, itemStack, slot, requirementText(itemStack.count, need.count))
    }

    private fun requirementText(have: Int, need: Int): String =
        if (have >= need) "${ChatFormatting.GREEN}$need" else "${ChatFormatting.RED}${need - have}"


    /**
     * The title and the inventory label in the theme's text colour.
     */
    override fun extractLabels(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        ScreenStyle.theme = theme
        graphics.text(font, title, titleLabelX, titleLabelY, ScreenStyle.TEXT, false)
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, ScreenStyle.TEXT, false)
    }

    /**
     * While a text box has focus it takes every key but escape, so typing the inventory key or a hotbar number does
     * not close the screen or move items.
     */
    override fun keyPressed(event: KeyEvent): Boolean {
        val box = focused as? EditBox
        if (box != null && box.canConsumeInput() && !event.isEscape) {
            box.keyPressed(event)
            return true
        }
        return super.keyPressed(event)
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

        /** How opaque the slot face is over a hint (0-255). */
        const val HINT_FADE = 0xC8
    }
}
