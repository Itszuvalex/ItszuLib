package com.itszuvalex.itszulib.client.screen

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.InputWithModifiers
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

/**
 * A button drawn in the current [ScreenTheme] ([ScreenStyle.button]) instead of vanilla's sprite: raised, lit while
 * hovered or focused, pressed in while [selected] (e.g. the tab of the open panel), flat with muted text while
 * inactive. Shows [icon] (an item, centred) instead of its message when one is given; the message is still narrated.
 * [accent] names a [ButtonAccents] colour (from the theme) that tints it by what it does: IO, upgrades, danger, ...
 */
class ThemedButton @JvmOverloads constructor(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component,
    private val onPress: (ThemedButton) -> Unit,
    tooltip: Component? = null,
    private val icon: ItemStack = ItemStack.EMPTY,
    private val selected: () -> Boolean = { false },
    var accent: String? = null,
) : AbstractButton(x, y, width, height, message) {
    init {
        tooltip?.let { setTooltip(Tooltip.create(it)) }
    }

    override fun onPress(input: InputWithModifiers) = onPress(this)

    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        val state = when {
            !active -> ScreenStyle.ButtonState.INACTIVE
            selected() -> ScreenStyle.ButtonState.SELECTED
            isHoveredOrFocused -> ScreenStyle.ButtonState.HOVERED
            else -> ScreenStyle.ButtonState.IDLE
        }
        ScreenStyle.button(graphics, x, y, width, height, state, accent?.let(ScreenStyle.theme::accent))
        if (!icon.isEmpty) {
            graphics.fakeItem(icon, x + (width - 16) / 2, y + (height - 16) / 2)
            return
        }
        setFGColor(if (active) ScreenStyle.TEXT else ScreenStyle.TEXT_MUTED)
        extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE))
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) = defaultButtonNarrationText(output)
}
