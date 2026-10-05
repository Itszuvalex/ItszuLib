package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.channel.ChannelResources
import com.itszuvalex.itszulib.channel.ChannelScope
import com.itszuvalex.itszulib.menu.ChannelEntry
import com.itszuvalex.itszulib.menu.MenuActionPayload
import com.itszuvalex.itszulib.menu.MenuChannels
import com.itszuvalex.itszulib.menu.MenuTextActionPayload
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.client.network.ClientPacketDistributor

/**
 * Channels for a block ([MenuChannels]): which resource (when the block offers several), whose channels to look at (the
 * player's own or their team's), the channels with how many loaded blocks are on each, a click to join one, a button to
 * leave, a name to make a new one (which the block then joins), and a delete mark on each channel that asks before
 * deleting, saying whether blocks are using it. Draws [MenuChannels.view] and only sends requests; the server decides.
 */
class ChannelPanel(private val channels: MenuChannels, private val containerId: Int) : ScreenComponent(WIDTH, HEIGHT) {
    private var scroll = 0
    private var pendingDelete: ChannelEntry? = null
    private var nameBox: EditBox? = null
    private var confirm: ThemedButton? = null
    private var cancel: ThemedButton? = null
    private var typed = ""

    private val view get() = channels.view

    override fun init(host: ComponentHost) {
        val x0 = x + PAD
        val step = (WIDTH - 2 * PAD - 2 * ARROW - 2 * GAP)
        host.addHostWidget(ThemedButton(x0, y + PAD, ARROW, ROW_H + 2, Component.literal("<"), { cycleResource(-1) }, Component.translatable("gui.itszulib.channels.previous_resource")))
        host.addHostWidget(ThemedButton(x0 + ARROW + GAP + step + GAP, y + PAD, ARROW, ROW_H + 2, Component.literal(">"), { cycleResource(1) }, Component.translatable("gui.itszulib.channels.next_resource")))
        val half = (WIDTH - 2 * PAD - GAP) / 2
        host.addHostWidget(ThemedButton(x0, y + SCOPE_Y, half, ROW_H + 2, Component.translatable("gui.itszulib.channels.scope.player"), { setScope(ChannelScope.PLAYER) },
            Component.translatable("gui.itszulib.channels.scope.player.tooltip"), selected = { view.scope == ChannelScope.PLAYER }))
        host.addHostWidget(ThemedButton(x0 + half + GAP, y + SCOPE_Y, half, ROW_H + 2, Component.translatable("gui.itszulib.channels.scope.team"), { setScope(ChannelScope.TEAM) },
            Component.translatable("gui.itszulib.channels.scope.team.tooltip"), selected = { view.scope == ChannelScope.TEAM }))
        host.addHostWidget(ThemedButton(x0 + WIDTH - 2 * PAD - NONE_W, y + CURRENT_Y - 2, NONE_W, ROW_H + 2, Component.translatable("gui.itszulib.channels.none"),
            { text(MenuChannels.ACTION_SELECT, "") }, Component.translatable("gui.itszulib.channels.none.tooltip"), accent = ButtonAccents.DANGER))
        val box = EditBox(host.hostFont, x0 + 1, y + CREATE_Y, WIDTH - 2 * PAD - ARROW - GAP - 2, ROW_H + 2, nameBox, Component.translatable("gui.itszulib.channels.name"))
        box.setHint(Component.translatable("gui.itszulib.channels.name").withStyle(ChatFormatting.DARK_GRAY))
        box.setMaxLength(com.itszuvalex.itszulib.channel.ChannelState.MAX_NAME)
        if (nameBox == null) box.setValue(typed)
        box.setResponder { typed = it }
        nameBox = host.addHostWidget(box)
        host.addHostWidget(ThemedButton(x0 + WIDTH - 2 * PAD - ARROW, y + CREATE_Y - 1, ARROW, ROW_H + 2, Component.literal("+"), { create() },
            Component.translatable("gui.itszulib.channels.create"), accent = ButtonAccents.INFO))
        val buttonW = (WIDTH - 2 * PAD - 3 * GAP) / 2
        confirm = host.addHostWidget(ThemedButton(x0 + GAP, y + LIST_Y + 3 * ROW_H, buttonW, ROW_H + 3, Component.translatable("gui.itszulib.channels.delete"),
            { deleteConfirmed() }, accent = ButtonAccents.DANGER))
        cancel = host.addHostWidget(ThemedButton(x0 + GAP + buttonW + GAP, y + LIST_Y + 3 * ROW_H, buttonW, ROW_H + 3, Component.translatable("gui.itszulib.channels.cancel"),
            { pendingDelete = null }))
    }

    private fun send(action: Int, data: Int) = ClientPacketDistributor.sendToServer(MenuActionPayload(containerId, action, data))

    private fun text(action: Int, text: String) = ClientPacketDistributor.sendToServer(MenuTextActionPayload(containerId, action, text))

    private fun cycleResource(by: Int) {
        val n = view.resources.size
        if (n > 1) send(MenuChannels.ACTION_RESOURCE, Math.floorMod(view.resourceIndex + by, n))
        scroll = 0
        pendingDelete = null
    }

    private fun setScope(scope: ChannelScope) {
        send(MenuChannels.ACTION_SCOPE, scope.ordinal)
        scroll = 0
        pendingDelete = null
    }

    private fun create() {
        val name = typed.trim()
        if (name.isEmpty()) return
        text(MenuChannels.ACTION_CREATE, name)
        nameBox?.value = ""
        typed = ""
    }

    private fun deleteConfirmed() {
        val entry = pendingDelete ?: return
        text(MenuChannels.ACTION_DELETE, entry.id.toString())
        pendingDelete = null
    }

    private fun visibleEntries(): List<ChannelEntry> = view.channels

    private fun maxScroll() = (visibleEntries().size - ROWS).coerceAtLeast(0)

    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        ScreenStyle.panel(graphics, x, y, width, height)
        val font = host.hostFont
        val v = view
        val resource = v.resource
        val x0 = x + PAD
        val inner = WIDTH - 2 * PAD
        // The channel being asked about is dropped from the list if a delete was confirmed from another screen.
        if (pendingDelete != null && v.channels.none { it.id == pendingDelete?.id }) pendingDelete = null
        scroll = scroll.coerceIn(0, maxScroll())

        val title = if (resource == null) Component.translatable("gui.itszulib.channels.nothing") else ChannelResources.name(resource)
        graphics.centeredText(font, title, x + WIDTH / 2, y + PAD + 2, ScreenStyle.TEXT)

        val current = when {
            v.currentMissing -> Component.translatable("gui.itszulib.channels.current.missing", v.currentName, scopeName(v.currentScope))
            v.currentId == null -> Component.translatable("gui.itszulib.channels.current.none")
            else -> Component.translatable("gui.itszulib.channels.current", v.currentName, scopeName(v.currentScope))
        }
        graphics.text(font, font.plainSubstrByWidth(current.string, inner - NONE_W - GAP), x0, y + CURRENT_Y, if (v.currentMissing) 0xFFFFAA55.toInt() else ScreenStyle.TEXT, false)

        ScreenStyle.inset(graphics, x0, y + LIST_Y, inner, ROWS * ROW_H)
        val entries = visibleEntries()
        if (entries.isEmpty()) {
            val empty = if (v.scope == ChannelScope.TEAM && !v.inTeam) Component.translatable("gui.itszulib.channels.no_team") else Component.translatable("gui.itszulib.channels.empty")
            graphics.text(font, empty, x0 + 3, y + LIST_Y + 3, ScreenStyle.TEXT_MUTED, false)
        }
        for (row in 0 until ROWS) {
            val entry = entries.getOrNull(scroll + row) ?: break
            val top = y + LIST_Y + row * ROW_H
            val selected = entry.id == v.currentId
            if (selected) graphics.fill(x0 + 1, top, x0 + inner - 1, top + ROW_H, (ScreenStyle.PROGRESS and 0xFFFFFF) or (0x55 shl 24))
            else if (pendingDelete == null && mouseX in x0..(x0 + inner) && mouseY in top until top + ROW_H) graphics.fill(x0 + 1, top, x0 + inner - 1, top + ROW_H, 0x22FFFFFF)
            graphics.text(font, font.plainSubstrByWidth(entry.name, inner - 36), x0 + 3, top + 2, ScreenStyle.TEXT, false)
            val count = entry.count.toString()
            graphics.text(font, count, x0 + inner - 12 - font.width(count), top + 2, ScreenStyle.TEXT_MUTED, false)
            graphics.text(font, "x", x0 + inner - 9, top + 2, if (mouseX in (x0 + inner - 11)..(x0 + inner - 1) && mouseY in top until top + ROW_H) 0xFFFF5555.toInt() else ScreenStyle.TEXT_MUTED, false)
        }
        if (entries.size > ROWS) {
            val barH = ROWS * ROW_H
            val thumb = (barH * ROWS / entries.size).coerceAtLeast(6)
            val thumbY = y + LIST_Y + (barH - thumb) * scroll / maxScroll().coerceAtLeast(1)
            graphics.fill(x0 + inner - 2, thumbY, x0 + inner, thumbY + thumb, ScreenStyle.TEXT_MUTED)
        }
        if (v.message.isNotEmpty()) graphics.text(font, font.plainSubstrByWidth(v.message, inner), x0, y + MESSAGE_Y, 0xFFFF5555.toInt(), false)

        val pending = pendingDelete
        confirm?.visible = pending != null
        cancel?.visible = pending != null
        if (pending != null) {
            ScreenStyle.panel(graphics, x0, y + LIST_Y, inner, ROWS * ROW_H)
            val message = if (pending.count > 0) Component.translatable("gui.itszulib.channels.confirm.in_use", pending.name, pending.count)
            else Component.translatable("gui.itszulib.channels.confirm", pending.name)
            graphics.textWithWordWrap(font, message, x0 + GAP, y + LIST_Y + GAP, inner - 2 * GAP, ScreenStyle.TEXT, false)
        }
    }

    private fun scopeName(scope: ChannelScope) = Component.translatable("gui.itszulib.channels.scope.${scope.name.lowercase()}")

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val x0 = x + PAD
        val inner = WIDTH - 2 * PAD
        val mx = event.x()
        val my = event.y()
        if (my < y + LIST_Y || my >= y + LIST_Y + ROWS * ROW_H || mx < x0 || mx >= x0 + inner) return false
        nameBox?.isFocused = false
        if (pendingDelete != null) return true
        val entry = visibleEntries().getOrNull(scroll + ((my - (y + LIST_Y)) / ROW_H).toInt()) ?: return true
        if (mx >= x0 + inner - 11) pendingDelete = entry else text(MenuChannels.ACTION_SELECT, entry.id.toString())
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!contains(mouseX, mouseY) || scrollY == 0.0) return false
        scroll = (scroll + if (scrollY < 0) 1 else -1).coerceIn(0, maxScroll())
        return true
    }

    companion object {
        const val WIDTH = 150
        const val PAD = 6
        const val GAP = 3
        const val ARROW = 12
        const val ROW_H = 11
        const val ROWS = 6
        const val NONE_W = 34
        const val SCOPE_Y = PAD + ROW_H + 6
        const val CURRENT_Y = SCOPE_Y + ROW_H + 8
        const val LIST_Y = CURRENT_Y + ROW_H + 4
        const val MESSAGE_Y = LIST_Y + ROWS * ROW_H + 3
        const val CREATE_Y = MESSAGE_Y + ROW_H + 2
        const val HEIGHT = CREATE_Y + ROW_H + 2 + PAD
    }
}
