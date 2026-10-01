package com.itszuvalex.itszulib.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.fluids.FluidStack
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Drawing helpers for container screens: fluid tanks, progress bars and their tooltips. Port of the parts of ItszuLib
 * 1.12.2's GUI toolkit that machine screens use (`GuiFluidTank`, `GuiProgress`, tooltips); the widget toolkit
 * itself (panels, flow layouts, text boxes) is not ported, since 26.1 screens have their own widgets.
 *
 * Client only. Coordinates are in the graphics' current pose (screen coordinates, or relative to the screen's corner
 * inside `extractLabels`).
 */
object ScreenHelpers {
    /**
     * Fills a [width] x [height] tank at ([x], [y]) from the bottom with [stack]'s still texture, tinted, tiled in
     * 16-pixel squares, in proportion to [capacity].
     */
    @JvmStatic
    fun fluidTank(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, stack: FluidStack, capacity: Int) {
        val filled = ScreenMath.filledPixels(stack.amount.toLong(), capacity.toLong(), height)
        if (filled <= 0 || stack.isEmpty) return
        val model = Minecraft.getInstance().modelManager.fluidStateModelSet.get(stack.fluid.defaultFluidState())
        val sprite = model.stillMaterial().sprite()
        val tint = model.fluidTintSource()?.colorAsStack(stack) ?: -1
        val color = if (tint ushr 24 == 0) tint or (0xFF shl 24) else tint
        val top = y + height - filled
        graphics.enableScissor(x, top, x + width, y + height)
        var tileY = y + height - TILE
        while (tileY + TILE > top) {
            var tileX = x
            while (tileX < x + width) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tileX, tileY, TILE, TILE, color)
                tileX += TILE
            }
            tileY -= TILE
        }
        graphics.disableScissor()
    }

    /**
     * A solid progress bar: [background] over the whole area, [color] over the done part. Fills left to right, or
     * bottom to top if [vertical].
     */
    @JvmStatic
    @JvmOverloads
    fun progressBar(
        graphics: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fraction: Double,
        color: Int,
        background: Int = 0,
        vertical: Boolean = false,
    ) {
        if (background ushr 24 != 0) graphics.fill(x, y, x + width, y + height, background)
        if (vertical) {
            val done = ScreenMath.fractionPixels(fraction, height)
            if (done > 0) graphics.fill(x, y + height - done, x + width, y + height, color)
        } else {
            val done = ScreenMath.fractionPixels(fraction, width)
            if (done > 0) graphics.fill(x, y, x + done, y + height, color)
        }
    }

    /**
     * Draws the done part of a progress sprite (e.g. a furnace arrow) from the GUI sprite atlas, clipped to [fraction]:
     * left to right, or bottom to top if [vertical].
     */
    @JvmStatic
    @JvmOverloads
    fun progressSprite(graphics: GuiGraphicsExtractor, sprite: Identifier, x: Int, y: Int, width: Int, height: Int, fraction: Double, vertical: Boolean = false) {
        if (vertical) {
            val done = ScreenMath.fractionPixels(fraction, height)
            if (done <= 0) return
            graphics.enableScissor(x, y + height - done, x + width, y + height)
        } else {
            val done = ScreenMath.fractionPixels(fraction, width)
            if (done <= 0) return
            graphics.enableScissor(x, y, x + done, y + height)
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height)
        graphics.disableScissor()
    }

    /**
     * Sets [lines] as the tooltip if the mouse is over the area.
     *
     * @return True if it was.
     */
    @JvmStatic
    fun tooltipIfHovered(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int, lines: List<Component>): Boolean {
        if (!ScreenMath.isHovering(mouseX, mouseY, x, y, width, height)) return false
        graphics.setTooltipForNextFrame(Minecraft.getInstance().font, lines, java.util.Optional.empty(), mouseX, mouseY)
        return true
    }

    /**
     * Tank tooltip: the fluid's name (or "Empty") and `amount / capacity mB`.
     */
    @JvmStatic
    fun fluidTooltip(stack: FluidStack, capacity: Int): List<Component> = listOf(
        if (stack.isEmpty) Component.translatable("gui.itszulib.tank.empty") else stack.hoverName,
        Component.translatable("gui.itszulib.tank.amount", ScreenMath.formatAmount(stack.amount.toLong()), ScreenMath.formatAmount(capacity.toLong())),
    )

    /**
     * Energy tooltip: `stored / max unit`.
     */
    @JvmStatic
    fun energyTooltip(stored: Double, max: Double, unit: Component): List<Component> = listOf(
        Component.translatable("gui.itszulib.energy", ScreenMath.formatAmount(stored.roundToInt().toLong()), ScreenMath.formatAmount(max.roundToInt().toLong()), unit),
    )

    private const val TILE = 16
}

/**
 * The arithmetic behind [ScreenHelpers], kept free of client classes so it can be unit tested.
 */
object ScreenMath {
    /**
     * Pixels of a [size]-pixel gauge filled by [amount] of [capacity]: rounded up, so any amount shows at least one
     * pixel; clamped to [0, size].
     */
    @JvmStatic
    fun filledPixels(amount: Long, capacity: Long, size: Int): Int {
        if (amount <= 0 || capacity <= 0 || size <= 0) return 0
        if (amount >= capacity) return size
        return ceil(amount.toDouble() * size / capacity).toInt().coerceIn(0, size)
    }

    /**
     * Pixels of a [size]-pixel bar done at [fraction] (clamped to [0, 1], rounded down so it only fills when done).
     */
    @JvmStatic
    fun fractionPixels(fraction: Double, size: Int): Int {
        if (size <= 0 || fraction.isNaN()) return 0
        return (fraction.coerceIn(0.0, 1.0) * size).toInt()
    }

    @JvmStatic
    fun isHovering(mouseX: Int, mouseY: Int, x: Int, y: Int, width: Int, height: Int): Boolean =
        mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height

    /**
     * Groups thousands with commas, e.g. 12,345.
     */
    @JvmStatic
    fun formatAmount(amount: Long): String = "%,d".format(java.util.Locale.ROOT, amount)
}
