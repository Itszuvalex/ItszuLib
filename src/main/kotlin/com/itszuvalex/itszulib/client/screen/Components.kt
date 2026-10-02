package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.client.ScreenHelpers
import com.itszuvalex.itszulib.menu.EnergyView
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.fluids.FluidStack

/**
 * Drawing in the current [ScreenTheme]: a [ComponentScreen] sets [theme] to its theme ([ComponentScreen.theme]) before
 * it draws, and components read their colours here, so they follow it.
 */
object ScreenStyle {
    /** The theme being drawn with. */
    @JvmStatic
    var theme: ScreenTheme = ScreenTheme.LIGHT

    val PANEL: Int get() = theme.panel
    val FRAME: Int get() = theme.frame
    val TEXT: Int get() = theme.text
    val TEXT_MUTED: Int get() = theme.textMuted

    /** The empty part of gauges and bars. */
    val DARK: Int get() = theme.well
    val ENERGY: Int get() = theme.energy
    val PROGRESS: Int get() = theme.progress

    /**
     * A panel: an outline with cut corners, a bevel (light top and left, dark bottom and right), the panel colour and
     * its grain ([ScreenGrain]).
     */
    @JvmStatic
    fun panel(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int) {
        val t = theme
        val r = x + width
        val b = y + height
        graphics.fill(x + 1, y, r - 1, y + 1, t.outline)
        graphics.fill(x + 1, b - 1, r - 1, b, t.outline)
        graphics.fill(x, y + 1, x + 1, b - 1, t.outline)
        graphics.fill(r - 1, y + 1, r, b - 1, t.outline)
        graphics.fill(x + 1, y + 1, r - 1, b - 1, t.panel)
        graphics.fill(x + 1, y + 1, r - 2, y + 2, t.panelLight)
        graphics.fill(x + 1, y + 1, x + 2, b - 2, t.panelLight)
        graphics.fill(x + 2, b - 2, r - 1, b - 1, t.panelDark)
        graphics.fill(r - 2, y + 2, r - 1, b - 1, t.panelDark)
        ScreenGrain.draw(graphics, x + 2, y + 2, width - 4, height - 4, t.grain)
    }

    /**
     * A slot's inset for a slot at ([x], [y]) (its 16x16 item area): dark top and left edges, light bottom and right.
     */
    @JvmStatic
    fun slot(graphics: GuiGraphicsExtractor, x: Int, y: Int) = inset(graphics, x - 1, y - 1, 18, 18)

    /**
     * An inset area (a slot, a tank): its face, dark top and left edges, light bottom and right.
     */
    @JvmStatic
    fun inset(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int) {
        val t = theme
        graphics.fill(x, y, x + width, y + height, t.slot)
        graphics.fill(x, y, x + width - 1, y + 1, t.slotShadow)
        graphics.fill(x, y, x + 1, y + height - 1, t.slotShadow)
        graphics.fill(x + 1, y + height - 1, x + width, y + height, t.slotLight)
        graphics.fill(x + width - 1, y + 1, x + width, y + height, t.slotLight)
    }

    /**
     * The ring marking a take-only slot at ([x], [y]); drawn before the slots, so neighbouring output slots share one
     * outline.
     */
    @JvmStatic
    fun outputRing(graphics: GuiGraphicsExtractor, x: Int, y: Int) = graphics.fill(x - 2, y - 2, x + 18, y + 18, theme.slotOutput)

    /**
     * A one-pixel frame around the area.
     */
    @JvmStatic
    fun frame(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int) = graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, theme.frame)
}

/**
 * A vertical energy gauge with a `stored / capacity unit` tooltip, over a synced [EnergyView]
 * ([com.itszuvalex.itszulib.menu.MenuCore.syncEnergy] for an ItszuLib battery,
 * [com.itszuvalex.itszulib.menu.MenuCore.syncEnergyHandler] for a NeoForge energy handler).
 */
class EnergyGauge @JvmOverloads constructor(
    private val view: () -> EnergyView,
    private val unit: Component = Component.translatable("gui.itszulib.energy.unit"),
    private val color: Int? = null,
    width: Int = 8,
    height: Int = 52,
) : ScreenComponent(width, height) {
    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        val energy = view()
        ScreenStyle.frame(graphics, x, y, width, height)
        ScreenHelpers.progressBar(graphics, x, y, width, height, energy.fraction, color ?: ScreenStyle.ENERGY, ScreenStyle.DARK, vertical = true)
        ScreenHelpers.tooltipIfHovered(graphics, mouseX, mouseY, x, y, width, height, ScreenHelpers.energyTooltip(energy.stored, energy.capacity, unit))
    }
}

/**
 * A fluid tank gauge with the fluid's name and `amount / capacity mB` as its tooltip. [fluid] is usually the client copy
 * kept by [com.itszuvalex.itszulib.menu.MenuSyncs.fluid].
 */
class FluidGauge @JvmOverloads constructor(
    private val fluid: () -> FluidStack,
    private val capacity: () -> Int,
    width: Int = 16,
    height: Int = 52,
) : ScreenComponent(width, height) {
    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        val stack = fluid()
        ScreenStyle.frame(graphics, x, y, width, height)
        graphics.fill(x, y, x + width, y + height, ScreenStyle.DARK)
        ScreenHelpers.fluidTank(graphics, x, y, width, height, stack, capacity())
        ScreenHelpers.tooltipIfHovered(graphics, mouseX, mouseY, x, y, width, height, ScreenHelpers.fluidTooltip(stack, capacity()))
    }
}

/**
 * A progress bar filled to [fraction] (left to right, or bottom to top if [vertical]).
 */
class ProgressBar @JvmOverloads constructor(
    private val fraction: () -> Double,
    width: Int,
    height: Int,
    private val color: Int? = null,
    private val vertical: Boolean = false,
) : ScreenComponent(width, height) {
    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        ScreenStyle.frame(graphics, x, y, width, height)
        ScreenHelpers.progressBar(graphics, x, y, width, height, fraction(), color ?: ScreenStyle.PROGRESS, ScreenStyle.DARK, vertical)
    }
}

/**
 * A line of text, re-read every frame, in the theme's text colour unless [color] is given.
 */
class Label @JvmOverloads constructor(private val text: () -> Component, private val color: Int? = null) : ScreenComponent(0, 9) {
    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        graphics.text(host.hostFont, text(), x, y, color ?: ScreenStyle.TEXT, false)
    }
}
