package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.channel.ChannelResources
import com.itszuvalex.itszulib.client.ScreenHelpers
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
    private var dragging = false
    private var dragOffset = 0

    /** A channel to bring into view once it is in the list (one just made), and how many frames to wait for it. */
    private var reveal: String? = null
    private var revealFrames = 0
    private var revealedCurrent = false

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
        confirm = host.addHostWidget(ThemedButton(x0 + GAP, y + LIST_Y + ROWS * ROW_H - ROW_H - 6, buttonW, ROW_H + 3, Component.translatable("gui.itszulib.channels.delete"),
            { deleteConfirmed() }, accent = ButtonAccents.DANGER))
        cancel = host.addHostWidget(ThemedButton(x0 + GAP + buttonW + GAP, y + LIST_Y + ROWS * ROW_H - ROW_H - 6, buttonW, ROW_H + 3, Component.translatable("gui.itszulib.channels.cancel"),
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
        reveal = name
        revealFrames = 0
    }

    private fun deleteConfirmed() {
        val entry = pendingDelete ?: return
        text(MenuChannels.ACTION_DELETE, entry.id.toString())
        pendingDelete = null
    }

    private fun visibleEntries(): List<ChannelEntry> = view.channels

    /** Closes a delete confirmation without deleting (for the showcase). */
    fun cancelAsk() {
        pendingDelete = null
    }

    /** Asks before deleting the first channel in the list, as clicking its mark does (for the showcase). */
    fun askDeleteFirst() {
        pendingDelete = visibleEntries().firstOrNull()
    }

    /** Scrolls so row [index] is the first shown, as far as the list allows (for the showcase). */
    fun scrollTo(index: Int) {
        scroll = ListScroll.clamp(index, visibleEntries().size, ROWS)
    }

    private fun needsBar() = ListScroll.needsBar(visibleEntries().size, ROWS)

    private fun trackTop() = y + LIST_Y

    private fun thumbHeight() = ListScroll.thumbHeight(ROWS * ROW_H, visibleEntries().size, ROWS)

    private fun thumbTop() = trackTop() + ListScroll.thumbTop(ROWS * ROW_H, thumbHeight(), scroll, visibleEntries().size, ROWS)

    /** The part of the list that shows rows: narrower while a scrollbar takes the right edge. */
    private fun listWidth() = WIDTH - 2 * PAD - if (needsBar()) BAR + 1 else 0

    /** Brings the channel just made, and at first the one the block is on, into view as soon as they are in the list. */
    private fun revealWanted(entries: List<ChannelEntry>) {
        val wanted = reveal ?: view.currentName.takeIf { !revealedCurrent && view.currentId != null }
        if (wanted == null) return
        val index = entries.indexOfFirst { it.name.equals(wanted, ignoreCase = true) }
        if (index >= 0) {
            scroll = ListScroll.reveal(index, scroll, entries.size, ROWS)
            reveal = null
            revealedCurrent = true
        } else if (reveal != null && ++revealFrames > 100) reveal = null
    }

    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        ScreenStyle.panel(graphics, x, y, width, height)
        val font = host.hostFont
        val v = view
        val resource = v.resource
        val x0 = x + PAD
        val inner = WIDTH - 2 * PAD
        // The channel being asked about is dropped from the list if a delete was confirmed from another screen.
        if (pendingDelete != null && v.channels.none { it.id == pendingDelete?.id }) pendingDelete = null
        revealWanted(visibleEntries())
        scroll = ListScroll.clamp(scroll, visibleEntries().size, ROWS)

        val title = if (resource == null) Component.translatable("gui.itszulib.channels.nothing") else ChannelResources.name(resource)
        graphics.centeredText(font, title, x + WIDTH / 2, y + PAD + 2, ScreenStyle.TEXT)

        val current = when {
            v.currentMissing -> Component.translatable("gui.itszulib.channels.current.missing", v.currentName, scopeName(v.currentScope))
            v.currentId == null -> Component.translatable("gui.itszulib.channels.current.none")
            else -> Component.translatable("gui.itszulib.channels.current", v.currentName, scopeName(v.currentScope))
        }
        graphics.text(font, font.plainSubstrByWidth(current.string, inner - NONE_W - GAP), x0, y + CURRENT_Y, if (v.currentMissing) 0xFFFFAA55.toInt() else ScreenStyle.TEXT, false)

        ScreenStyle.inset(graphics, x0, y + LIST_Y, inner, ROWS * ROW_H)
        // Lighter than a slot, so the names read against it.
        graphics.fill(x0 + 1, y + LIST_Y + 1, x0 + inner - 1, y + LIST_Y + ROWS * ROW_H - 1, ScreenStyle.mix(ScreenStyle.theme.slot, ScreenStyle.theme.panel, 0.8f))
        val entries = visibleEntries()
        if (entries.isEmpty()) {
            val empty = if (v.scope == ChannelScope.TEAM && !v.inTeam) Component.translatable("gui.itszulib.channels.no_team") else Component.translatable("gui.itszulib.channels.empty")
            graphics.text(font, empty, x0 + 3, y + LIST_Y + 3, ScreenStyle.TEXT_MUTED, false)
        }
        val listW = listWidth()
        val hovering = pendingDelete == null && !dragging
        for (row in 0 until ROWS) {
            val entry = entries.getOrNull(scroll + row) ?: break
            val top = y + LIST_Y + row * ROW_H
            val inRow = hovering && mouseX in x0 until (x0 + listW) && mouseY in top until top + ROW_H
            val onMark = inRow && mouseX >= x0 + listW - 11
            if (entry.id == v.currentId) graphics.fill(x0 + 1, top, x0 + listW - 1, top + ROW_H, (ScreenStyle.PROGRESS and 0xFFFFFF) or (0x55 shl 24))
            else if (inRow) graphics.fill(x0 + 1, top, x0 + listW - 1, top + ROW_H, 0x22FFFFFF)
            val nameWidth = listW - 3 - 4 - 12 - font.width(entry.count.toString()) - 4
            val shown = ListScroll.ellipsize(entry.name, nameWidth) { font.width(it) }
            graphics.text(font, shown, x0 + 3, top + 2, ScreenStyle.TEXT, false)
            val count = entry.count.toString()
            graphics.text(font, count, x0 + listW - 12 - font.width(count), top + 2, ScreenStyle.TEXT_MUTED, false)
            graphics.text(font, "x", x0 + listW - 9, top + 2, if (onMark) 0xFFFF5555.toInt() else ScreenStyle.TEXT_MUTED, false)
            if (onMark) ScreenHelpers.tooltipIfHovered(graphics, mouseX, mouseY, x0 + listW - 11, top, 11, ROW_H, listOf(Component.translatable("gui.itszulib.channels.delete")))
            else if (inRow) {
                val tip = ArrayList<Component>()
                if (shown != entry.name) tip += Component.literal(entry.name)
                tip += Component.translatable("gui.itszulib.channels.uses", entry.count).withStyle(ChatFormatting.GRAY)
                ScreenHelpers.tooltipIfHovered(graphics, mouseX, mouseY, x0, top, listW - 12, ROW_H, tip)
            }
        }
        if (needsBar()) {
            val trackX = x0 + inner - BAR
            graphics.fill(trackX, trackTop(), trackX + BAR, trackTop() + ROWS * ROW_H, ScreenStyle.DARK)
            val top = thumbTop()
            val hot = dragging || (mouseX in trackX until trackX + BAR && mouseY in top until top + thumbHeight())
            graphics.fill(trackX, top, trackX + BAR, top + thumbHeight(), if (hot) ScreenStyle.TEXT else ScreenStyle.TEXT_MUTED)
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
        if (my < trackTop() || my >= trackTop() + ROWS * ROW_H || mx < x0 || mx >= x0 + inner) return false
        nameBox?.isFocused = false
        if (pendingDelete != null) return true
        val count = visibleEntries().size
        // The scrollbar: grab the thumb, or click the track beside it to move a page that way.
        if (needsBar() && mx >= x0 + inner - BAR) {
            val top = thumbTop()
            if (my >= top && my < top + thumbHeight()) {
                dragging = true
                dragOffset = (my - top).toInt()
            } else {
                scroll = ListScroll.page(scroll, if (my < top) -1 else 1, count, ROWS)
            }
            return true
        }
        val listW = listWidth()
        if (mx >= x0 + listW) return true
        val entry = visibleEntries().getOrNull(scroll + ((my - trackTop()) / ROW_H).toInt()) ?: return true
        if (mx >= x0 + listW - 11) pendingDelete = entry else text(MenuChannels.ACTION_SELECT, entry.id.toString())
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (!dragging) return false
        val wanted = (event.y() - dragOffset - trackTop()).toInt()
        scroll = ListScroll.scrollForThumbTop(ROWS * ROW_H, thumbHeight(), wanted, visibleEntries().size, ROWS)
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (!dragging) return false
        dragging = false
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!contains(mouseX, mouseY) || scrollY == 0.0) return false
        scroll = ListScroll.clamp(scroll + if (scrollY < 0) 1 else -1, visibleEntries().size, ROWS)
        return true
    }

    companion object {
        const val WIDTH = 150
        const val PAD = 6
        const val GAP = 3
        const val ARROW = 12
        const val ROW_H = 11
        const val BAR = 5
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
