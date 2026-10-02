package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.client.ScreenHelpers
import com.itszuvalex.itszulib.menu.EnergyView
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.fluids.FluidStack

/**
 * The plain look ItszuLib's screens use by default: a light grey panel, darker slot and gauge frames.
 */
object ScreenStyle {
    const val PANEL = 0xFFC6C6C6.toInt()
    const val FRAME = 0xFF8B8B8B.toInt()
    const val TEXT = 0xFF404040.toInt()
    const val DARK = 0xFF2B2B2B.toInt()
    const val ENERGY = 0xFFCC3333.toInt()
    const val PROGRESS = 0xFF55DD55.toInt()

    @JvmStatic
    fun panel(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int) = graphics.fill(x, y, x + width, y + height, PANEL)

    /**
     * A slot's outline, for a slot at ([x], [y]).
     */
    @JvmStatic
    fun slot(graphics: GuiGraphicsExtractor, x: Int, y: Int) = graphics.fill(x - 1, y - 1, x + 17, y + 17, FRAME)

    /**
     * A one-pixel frame around the area.
     */
    @JvmStatic
    fun frame(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int) = graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, FRAME)
}

/**
 * A vertical energy gauge with a `stored / capacity unit` tooltip, over a synced [EnergyView]
 * ([com.itszuvalex.itszulib.menu.MenuCore.syncEnergy] for an ItszuLib battery,
 * [com.itszuvalex.itszulib.menu.MenuCore.syncEnergyHandler] for a NeoForge energy handler).
 */
class EnergyGauge @JvmOverloads constructor(
    private val view: () -> EnergyView,
    private val unit: Component = Component.translatable("gui.itszulib.energy.unit"),
    private val color: Int = ScreenStyle.ENERGY,
    width: Int = 8,
    height: Int = 52,
) : ScreenComponent(width, height) {
    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        val energy = view()
        ScreenStyle.frame(graphics, x, y, width, height)
        ScreenHelpers.progressBar(graphics, x, y, width, height, energy.fraction, color, ScreenStyle.DARK, vertical = true)
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
    private val color: Int = ScreenStyle.PROGRESS,
    private val vertical: Boolean = false,
) : ScreenComponent(width, height) {
    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        ScreenStyle.frame(graphics, x, y, width, height)
        ScreenHelpers.progressBar(graphics, x, y, width, height, fraction(), color, ScreenStyle.FRAME, vertical)
    }
}

/**
 * A line of text, re-read every frame.
 */
class Label(private val text: () -> Component, private val color: Int = ScreenStyle.TEXT) : ScreenComponent(0, 9) {
    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        graphics.text(host.hostFont, text(), x, y, color, false)
    }
}
