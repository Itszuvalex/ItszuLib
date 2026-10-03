package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.filter.FilterActions
import com.itszuvalex.itszulib.api.filter.FilterMode
import com.itszuvalex.itszulib.api.filter.ResourceFilter
import com.itszuvalex.itszulib.client.ScreenHelpers
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import java.util.Optional

/**
 * A [ResourceFilter]'s controls, click-to-set as in AE2: an allow / deny button, a component-matching button, and a row
 * of [cells] entry cells. Clicking a cell while holding something lists it ([com.itszuvalex.itszulib.api.filter.FilterKind.fromHeld]),
 * empty-handed clears it. Each click calls [send] with a [FilterActions] action, which the screen passes to its menu
 * (e.g. in a [com.itszuvalex.itszulib.menu.MenuActionPayload]) to apply with [ResourceFilter.apply].
 *
 * [filter] is read every frame; null (nothing to filter) dims the controls. Item, ItszuLib item and fluid entries draw
 * themselves; other kinds need [drawEntry] and [nameOf].
 */
class FilterRow @JvmOverloads constructor(
    private val filter: () -> ResourceFilter<*>?,
    private val send: (Int) -> Unit,
    val cells: Int = 9,
    private val drawEntry: ((GuiGraphicsExtractor, Any, Int, Int) -> Unit)? = null,
    private val nameOf: ((Any) -> Component?)? = null,
    private val setHint: Component = Component.translatable("gui.itszulib.filter.set"),
) : ScreenComponent(cells * CELL, BUTTON_H + GAP + CELL) {
    private fun modeRect() = intArrayOf(x, y, MODE_W, BUTTON_H)
    private fun componentsRect() = intArrayOf(x + MODE_W + GAP, y, COMPONENTS_W, BUTTON_H)
    private fun cellAt(mx: Double, my: Double): Int {
        val cy = y + BUTTON_H + GAP
        if (mx < x || my < cy || my >= cy + CELL) return -1
        val c = ((mx - x) / CELL).toInt()
        return if (c < cells) c else -1
    }

    private fun over(r: IntArray, mx: Number, my: Number) = mx.toDouble() >= r[0] && mx.toDouble() < r[0] + r[2] && my.toDouble() >= r[1] && my.toDouble() < r[1] + r[3]

    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        val font = host.hostFont
        val f = filter()
        fun button(r: IntArray, label: Component, accent: String) {
            val state = when {
                f == null -> ScreenStyle.ButtonState.INACTIVE
                over(r, mouseX, mouseY) -> ScreenStyle.ButtonState.HOVERED
                else -> ScreenStyle.ButtonState.IDLE
            }
            ScreenStyle.button(graphics, r[0], r[1], r[2], r[3], state, ScreenStyle.theme.accent(accent))
            graphics.text(font, label, r[0] + (r[2] - font.width(label)) / 2, r[1] + (r[3] - font.lineHeight) / 2 + 1, if (f == null) ScreenStyle.TEXT_MUTED else ScreenStyle.TEXT, false)
        }
        val deny = f?.mode == FilterMode.DENY
        button(modeRect(), Component.translatable(if (deny) "gui.itszulib.filter.deny" else "gui.itszulib.filter.allow"), if (deny) ButtonAccents.DANGER else ButtonAccents.IO)
        button(componentsRect(), Component.translatable(if (f?.matchComponents == false) "gui.itszulib.filter.any_data" else "gui.itszulib.filter.exact"), ButtonAccents.INFO)

        val cy = y + BUTTON_H + GAP
        for (i in 0 until cells) {
            val cx = x + i * CELL + 1
            ScreenStyle.slot(graphics, cx, cy + 1)
            val entry = f?.entries?.getOrNull(i)
            if (entry != null && !isEmptyEntry(entry)) draw(graphics, entry, cx, cy + 1)
            if (f == null || i >= f.size) graphics.fill(cx, cy + 1, cx + 16, cy + 17, (ScreenStyle.PANEL and 0xFFFFFF) or (0xA0 shl 24))
        }

        val tip = ArrayList<Component>()
        when {
            over(modeRect(), mouseX, mouseY) -> tip += Component.translatable(
                if (f == null) "gui.itszulib.filter.none_here" else if (deny) "gui.itszulib.filter.deny.tip" else "gui.itszulib.filter.allow.tip")
            over(componentsRect(), mouseX, mouseY) -> tip += Component.translatable(
                if (f == null) "gui.itszulib.filter.none_here" else if (f.matchComponents) "gui.itszulib.filter.exact.tip" else "gui.itszulib.filter.any_data.tip")
            else -> {
                val cell = cellAt(mouseX.toDouble(), mouseY.toDouble())
                if (cell >= 0) {
                    if (f == null || cell >= f.size) {
                        tip += Component.translatable("gui.itszulib.filter.none_here")
                    } else {
                        val entry = f.entries[cell]
                        val name = if (isEmptyEntry(entry)) null else name(entry)
                        tip += if (name != null) Component.translatable("gui.itszulib.filter.listed", name) else Component.translatable("gui.itszulib.filter.empty")
                        tip += setHint.copy().withStyle(ChatFormatting.GRAY)
                        if (f.isEmpty) tip += Component.translatable("gui.itszulib.filter.nothing").withStyle(ChatFormatting.GRAY)
                    }
                }
            }
        }
        if (tip.isNotEmpty()) graphics.setTooltipForNextFrame(font, tip, Optional.empty(), mouseX, mouseY)
    }

    private fun isEmptyEntry(entry: Any): Boolean = when (entry) {
        is ItemStack -> entry.isEmpty
        is FluidStack -> entry.isEmpty
        is IItemStack -> entry.isEmpty()
        else -> false
    }

    private fun draw(graphics: GuiGraphicsExtractor, entry: Any, x: Int, y: Int) {
        when (entry) {
            is ItemStack -> graphics.fakeItem(entry, x, y)
            is IItemStack -> graphics.fakeItem(entry.toMinecraft(), x, y)
            is FluidStack -> ScreenHelpers.fluidTank(graphics, x, y, 16, 16, entry, entry.amount)
            else -> drawEntry?.invoke(graphics, entry, x, y)
        }
    }

    private fun name(entry: Any): Component? = when (entry) {
        is ItemStack -> entry.hoverName
        is IItemStack -> entry.toMinecraft().hoverName
        is FluidStack -> entry.hoverName
        else -> nameOf?.invoke(entry)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val action = when {
            over(modeRect(), event.x(), event.y()) -> FilterActions.mode()
            over(componentsRect(), event.x(), event.y()) -> FilterActions.components()
            else -> cellAt(event.x(), event.y()).takeIf { it >= 0 }?.let(FilterActions::set)
        } ?: return false
        val f = filter() ?: return true
        if (FilterActions.op(action) == FilterActions.SET && FilterActions.cell(action) >= f.size) return true
        Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f))
        send(action)
        return true
    }

    companion object {
        const val CELL = 18
        const val BUTTON_H = 12
        const val GAP = 2
        const val MODE_W = 36
        const val COMPONENTS_W = 52
    }
}
