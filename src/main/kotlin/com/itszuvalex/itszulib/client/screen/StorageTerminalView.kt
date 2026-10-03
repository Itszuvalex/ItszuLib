package com.itszuvalex.itszulib.client.screen

import com.itszuvalex.itszulib.api.storage.SearchMode
import com.itszuvalex.itszulib.api.storage.SearchSubject
import com.itszuvalex.itszulib.api.storage.StorageEntries
import com.itszuvalex.itszulib.api.storage.StorageSearch
import com.itszuvalex.itszulib.api.storage.TerminalSort
import com.itszuvalex.itszulib.menu.MenuActionPayload
import com.itszuvalex.itszulib.menu.StorageTerminal
import com.itszuvalex.itszulib.menu.TerminalStack
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.neoforged.fml.ModList
import net.neoforged.neoforge.client.network.ClientPacketDistributor

/**
 * A storage terminal's screen part ([StorageTerminal], [com.itszuvalex.itszulib.menu.MenuCore.enableStorageTerminal]):
 * a search box, a grid of [columns] x [rows] item cells with their counts, and page buttons.
 *
 * Search terms are space separated and must all match; `-term` must not. A term matches the display name, or with a
 * prefix: `@` the mod (id or name), `#` the tooltip, `$` a tag, `*` the item id. The mode button picks what terms
 * without a prefix match; the sort button orders by count, name or id. The search runs here, over the list the server
 * sent, so it uses the client's language and needs no round trip.
 *
 * Clicking a cell takes a stack (right-click: half, shift: straight into the inventory); clicking the grid while
 * carrying something puts it in (right-click: one). The wheel turns pages.
 */
class StorageTerminalView @JvmOverloads constructor(
    private val terminal: StorageTerminal,
    val columns: Int = 9,
    val rows: Int = 6,
) : ScreenComponent(columns * CELL, HEADER + rows * CELL + FOOTER) {
    var query = ""
        private set
    var mode = SearchMode.NAME
        private set
    var sort = TerminalSort.COUNT
        private set
    var page = 0
        private set

    private var shown: List<IndexedValue<TerminalStack>> = emptyList()
    private var shownFor: Any? = null
    private val tooltips = HashMap<TerminalStack, List<String>>()
    private var tooltipsVersion = -1
    private var searchBox: EditBox? = null

    val pageSize: Int get() = columns * rows

    /** The entries matching the search, sorted, each with its place in [StorageTerminal.stacks]. */
    fun results(): List<IndexedValue<TerminalStack>> {
        val key = listOf(terminal.version, query, mode, sort)
        if (key != shownFor) {
            shownFor = key
            val search = StorageSearch.parse(query, mode)
            val matching = terminal.stacks.withIndex().filter { search.isEmpty || search.matches(Subject(it.value)) }
            shown = StorageEntries.sorted(matching, sort, { it.value.count }, { it.value.stack.hoverName.string }, { idOf(it.value).toString() })
        }
        return shown
    }

    fun pages(): Int = StorageEntries.pages(results().size, pageSize)

    fun setPage(value: Int) {
        page = value.coerceIn(0, pages() - 1)
    }

    override fun init(host: ComponentHost) {
        val font = host.hostFont
        val boxWidth = width - 2 * (BUTTON + GAP)
        val box = EditBox(font, x + 1, y + 1, boxWidth - 2, HEADER - 4, searchBox, Component.translatable("gui.itszulib.terminal.search"))
        box.setHint(Component.translatable("gui.itszulib.terminal.search").withStyle(ChatFormatting.DARK_GRAY))
        box.setMaxLength(MAX_QUERY)
        if (searchBox == null) box.setValue(query)
        box.setResponder { text ->
            query = text
            page = 0
        }
        searchBox = host.addHostWidget(box)
        val modeButton = ThemedButton(x + boxWidth + GAP, y, BUTTON, HEADER - 2, modeLabel(), { b ->
            mode = SearchMode.entries[(mode.ordinal + 1) % SearchMode.entries.size]
            page = 0
            b.message = modeLabel()
            b.setTooltip(Tooltip.create(modeTooltip()))
        }, modeTooltip(), accent = ButtonAccents.INFO)
        host.addHostWidget(modeButton)
        val sortButton = ThemedButton(x + width - BUTTON, y, BUTTON, HEADER - 2, sortLabel(), { b ->
            sort = TerminalSort.entries[(sort.ordinal + 1) % TerminalSort.entries.size]
            b.message = sortLabel()
            b.setTooltip(Tooltip.create(sortTooltip()))
        }, sortTooltip())
        host.addHostWidget(sortButton)
        val footerY = y + HEADER + rows * CELL + 2
        val prev = ThemedButton(x, footerY, BUTTON, FOOTER - 2, Component.literal("<"), { setPage(page - 1) }, Component.translatable("gui.itszulib.terminal.previous"))
        val next = ThemedButton(x + width - BUTTON, footerY, BUTTON, FOOTER - 2, Component.literal(">"), { setPage(page + 1) }, Component.translatable("gui.itszulib.terminal.next"))
        host.addHostWidget(prev)
        host.addHostWidget(next)
    }

    private fun modeLabel(): Component = Component.literal(mode.prefix?.toString() ?: "Aa")

    private fun modeTooltip(): Component = Component.translatable("gui.itszulib.terminal.mode", Component.translatable("gui.itszulib.terminal.mode.${mode.name.lowercase()}"))
        .append("\n").append(Component.translatable("gui.itszulib.terminal.syntax").withStyle(ChatFormatting.GRAY))

    private fun sortLabel(): Component = Component.literal(
        when (sort) {
            TerminalSort.COUNT -> "#"
            TerminalSort.NAME -> "Az"
            TerminalSort.ID -> "id"
        },
    )

    private fun sortTooltip(): Component = Component.translatable("gui.itszulib.terminal.sort", Component.translatable("gui.itszulib.terminal.sort.${sort.name.lowercase()}"))

    private fun gridY() = y + HEADER

    /** The cell under ([mx], [my]), or -1. */
    private fun cellAt(mx: Double, my: Double): Int {
        val cx = ((mx - x) / CELL).toInt()
        val cy = ((my - gridY()) / CELL).toInt()
        if (mx < x || my < gridY() || cx >= columns || cy >= rows) return -1
        return cy * columns + cx
    }

    override fun extract(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, host: ComponentHost) {
        val font = host.hostFont
        ScreenStyle.inset(graphics, x, y, width - 2 * (BUTTON + GAP), HEADER - 2)
        setPage(page)
        val entries = StorageEntries.page(results(), page, pageSize)
        for (i in 0 until pageSize) ScreenStyle.slot(graphics, x + (i % columns) * CELL + 1, gridY() + (i / columns) * CELL + 1)
        entries.forEachIndexed { i, e -> graphics.item(e.value.stack, x + (i % columns) * CELL + 1, gridY() + (i / columns) * CELL + 1) }
        graphics.nextStratum()
        entries.forEachIndexed { i, e ->
            val text = StorageEntries.formatCount(e.value.count)
            val tx = x + (i % columns) * CELL + CELL - 1
            val ty = gridY() + (i / columns) * CELL + CELL - 1
            val pose = graphics.pose()
            pose.pushMatrix()
            pose.translate(tx.toFloat(), ty.toFloat())
            pose.scale(COUNT_SCALE, COUNT_SCALE)
            graphics.text(font, text, -font.width(text), -font.lineHeight + 1, 0xFFFFFFFF.toInt(), true)
            pose.popMatrix()
        }
        val hovered = cellAt(mouseX.toDouble(), mouseY.toDouble())
        if (hovered >= 0) {
            val hx = x + (hovered % columns) * CELL + 1
            val hy = gridY() + (hovered / columns) * CELL + 1
            graphics.fill(hx, hy, hx + 16, hy + 16, HOVER)
            entries.getOrNull(hovered)?.let { e ->
                val lines = ArrayList(Screen.getTooltipFromItem(Minecraft.getInstance(), e.value.stack))
                lines += Component.translatable("gui.itszulib.terminal.count", "%,d".format(e.value.count)).withStyle(ChatFormatting.GRAY)
                graphics.setTooltipForNextFrame(font, lines, e.value.stack.tooltipImage, e.value.stack, mouseX, mouseY)
            }
        }
        val label = Component.translatable("gui.itszulib.terminal.page", page + 1, pages())
        val footerY = y + HEADER + rows * CELL + 2
        graphics.text(font, label, x + (width - font.width(label)) / 2, footerY + (FOOTER - 2 - font.lineHeight) / 2 + 1, ScreenStyle.TEXT, false)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val cell = cellAt(event.x(), event.y())
        if (cell < 0) return false
        searchBox?.isFocused = false
        val right = event.button() == 1
        val menu = Minecraft.getInstance().player?.containerMenu ?: return true
        if (!menu.carried.isEmpty) {
            send(menu.containerId, StorageTerminal.ACTION_INSERT, if (right) StorageTerminal.INSERT_ONE else 0)
            return true
        }
        val entry = StorageEntries.page(results(), page, pageSize).getOrNull(cell) ?: return true
        send(menu.containerId, StorageTerminal.ACTION_EXTRACT, StorageTerminal.extractData(entry.index, event.hasShiftDown(), right))
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!contains(mouseX, mouseY) || scrollY == 0.0) return false
        setPage(page + if (scrollY < 0) 1 else -1)
        return true
    }

    private fun send(containerId: Int, action: Int, data: Int) = ClientPacketDistributor.sendToServer(MenuActionPayload(containerId, action, data))

    private fun idOf(stack: TerminalStack): Identifier = BuiltInRegistries.ITEM.getKey(stack.stack.item)

    /** A synced entry as the search sees it; its tooltip is read once per list the server sends. */
    private inner class Subject(private val entry: TerminalStack) : SearchSubject {
        override val id: Identifier get() = idOf(entry)

        override fun name(): String = entry.stack.hoverName.string

        override fun modName(): String = ModList.get().getModContainerById(id.namespace).map { it.modInfo.displayName }.orElse(id.namespace)

        override fun tags(): Collection<Identifier> = entry.stack.item.builtInRegistryHolder().tags().map { it.location() }.toList()

        override fun tooltip(): List<String> {
            if (tooltipsVersion != terminal.version) {
                tooltips.clear()
                tooltipsVersion = terminal.version
            }
            return tooltips.getOrPut(entry) {
                val mc = Minecraft.getInstance()
                entry.stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL).map { it.string }
            }
        }
    }

    companion object {
        const val CELL = 18
        const val HEADER = 14
        const val FOOTER = 14
        const val BUTTON = 14
        const val GAP = 2
        const val MAX_QUERY = 128
        const val COUNT_SCALE = 0.5f
        private const val HOVER = 0x80FFFFFF.toInt()
    }
}
