package com.itszuvalex.itszulib.menu

import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.storage.ItemStorageIndex
import com.itszuvalex.itszulib.api.storage.StorageEntries
import com.itszuvalex.itszulib.api.storage.StorageEntry
import com.itszuvalex.itszulib.api.storage.StorageSearch
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/**
 * One kind of stack in a terminal's storage, as the client sees it: [stack] (one item, with its components) and how
 * many are held.
 */
class TerminalStack(@JvmField val stack: ItemStack, @JvmField val count: Long) {
    fun sameAs(other: TerminalStack): Boolean = count == other.count && ItemStack.isSameItemSameComponents(stack, other.stack)

    companion object {
        @JvmField
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TerminalStack> = StreamCodec.composite(
            ItemStack.STREAM_CODEC, TerminalStack::stack,
            ByteBufCodecs.VAR_LONG, TerminalStack::count,
            ::TerminalStack,
        )

        @JvmField
        val LIST_CODEC: StreamCodec<RegistryFriendlyByteBuf, List<TerminalStack>> = STREAM_CODEC.apply(ByteBufCodecs.list())
    }
}

/**
 * A menu's view of an [ItemStorageIndex] (one or many [com.itszuvalex.itszulib.api.storage.IndexedItemStorage]s, e.g. a
 * vault's), made with [MenuCore.enableStorageTerminal]. The server lists what is held, one entry per kind of stack with
 * its count, through the index (only slots holding something are read), and syncs the list while it changes (checked
 * every [REFRESH_TICKS], and at once after the player takes or puts something). The client searches, sorts and pages
 * that list itself ([com.itszuvalex.itszulib.client.screen.StorageTerminalView]): names and tooltips are the client's
 * own (the server has no mod translations), and typing needs no round trip.
 *
 * Items are taken with [ACTION_EXTRACT] (an entry's place in [stacks], a whole stack, half of one, or shift for
 * straight into the player's inventory) and put in with [ACTION_INSERT] (the carried stack, or one of it); shift-clicks
 * from the player's inventory go in ([quickMove]).
 */
class StorageTerminal(private val menu: MenuCore, private val index: () -> ItemStorageIndex?) {
    /** Server: the entries last listed (and so last sent). Client: what the server sent. */
    var stacks: List<TerminalStack> = emptyList()
        private set

    /** Client: counts up each time [stacks] arrives, so views know to search again. */
    var version = 0
        private set

    private var ticks = 0
    private var dirty = true

    /** The sync carrying [stacks]; added by [MenuCore.enableStorageTerminal]. */
    fun viewSync(): MenuSync<List<TerminalStack>> = MenuSync(
        { refresh() },
        { stacks = it; version++ },
        TerminalStack.LIST_CODEC,
        { a, b -> a.size == b.size && a.indices.all { a[it].sameAs(b[it]) } },
    )

    /** Asks for the list to be read again at the next sync. */
    fun markDirty() {
        dirty = true
    }

    private fun refresh(): List<TerminalStack> {
        if (dirty || ++ticks >= REFRESH_TICKS) {
            dirty = false
            ticks = 0
            stacks = index()?.let { list(it) } ?: emptyList()
        }
        return stacks
    }

    /**
     * Server side: a terminal action. @return False if it did nothing (an unknown entry, nothing carried).
     */
    fun handleAction(player: Player, action: Int, data: Int): Boolean {
        val idx = index() ?: return false
        val done = when (action) {
            ACTION_EXTRACT -> extract(player, idx, data and ENTRY_MASK, (data ushr FLAG_SHIFT) and 1 != 0, (data ushr FLAG_SHIFT) and 2 != 0)
            ACTION_INSERT -> insert(idx, one = data == INSERT_ONE)
            else -> false
        }
        if (done) markDirty()
        return done
    }

    private fun extract(player: Player, idx: ItemStorageIndex, entry: Int, toInventory: Boolean, half: Boolean): Boolean {
        val wanted = stacks.getOrNull(entry) ?: return false
        if (!toInventory && !menu.carried.isEmpty) return false
        var amount = minOf(wanted.stack.maxStackSize.toLong(), wanted.count).toInt()
        if (half) amount = (amount + 1) / 2
        val item = IItemStack.of(wanted.stack)
        val taken = idx.extract(item.item(), amount) { it.components() == item.components() }
        if (taken.isEmpty()) return false
        val stack = taken.toMinecraft()
        if (toInventory) {
            player.inventory.add(stack)
            if (!stack.isEmpty) idx.insert(IItemStack.of(stack))
        } else {
            menu.setCarried(stack)
        }
        return true
    }

    private fun insert(idx: ItemStorageIndex, one: Boolean): Boolean {
        val carried = menu.carried
        if (carried.isEmpty) return false
        val offered = if (one) carried.copyWithCount(1) else carried.copy()
        val left = idx.insert(IItemStack.of(offered))
        val used = offered.count - left.stackSize()
        if (used <= 0) return false
        carried.shrink(used)
        menu.setCarried(if (carried.isEmpty) ItemStack.EMPTY else carried)
        return true
    }

    /**
     * Server side: a shift-click on one of the player's slots puts its stack into the storage.
     *
     * @return Always empty, so vanilla does not repeat the move.
     */
    fun quickMove(player: Player, slot: Slot): ItemStack {
        val idx = index() ?: return ItemStack.EMPTY
        val stack = slot.item
        val left = idx.insert(IItemStack.of(stack.copy()))
        if (left.stackSize() == stack.count) return ItemStack.EMPTY
        if (left.isEmpty()) slot.setByPlayer(ItemStack.EMPTY) else slot.set(left.toMinecraft())
        slot.onTake(player, stack)
        markDirty()
        return ItemStack.EMPTY
    }

    companion object {
        /** How often (in ticks) an open terminal reads its storage again. */
        const val REFRESH_TICKS = 10

        /**
         * Takes from the entry at `data and ENTRY_MASK` of [stacks]: a stack (up to its maximum) to the carried slot, or
         * with [EXTRACT_TO_INVENTORY] into the player's inventory; with [EXTRACT_HALF], half of that.
         */
        const val ACTION_EXTRACT = -10

        /** Puts the carried stack in ([INSERT_ONE]: one of it). */
        const val ACTION_INSERT = -11

        @JvmField
        val ACTIONS = ACTION_INSERT..ACTION_EXTRACT

        const val ENTRY_MASK = 0xFFFFFF
        private const val FLAG_SHIFT = 24
        const val EXTRACT_TO_INVENTORY = 1 shl FLAG_SHIFT
        const val EXTRACT_HALF = 2 shl FLAG_SHIFT
        const val INSERT_ONE = 1

        /** The [ACTION_EXTRACT] data for entry [entry]. */
        @JvmStatic
        fun extractData(entry: Int, toInventory: Boolean, half: Boolean): Int =
            (entry and ENTRY_MASK) or (if (toInventory) EXTRACT_TO_INVENTORY else 0) or (if (half) EXTRACT_HALF else 0)

        /** Everything [index] holds, a kind of stack per entry, ordered by item id. */
        @JvmStatic
        fun list(index: ItemStorageIndex): List<TerminalStack> =
            StorageEntries.collect(index, StorageSearch.EMPTY, { error("no search") }, { error("no search") })
                .sortedBy { it.stack.item().toString() }
                .map { e: StorageEntry -> TerminalStack(e.stack.toMinecraft().copyWithCount(1), e.count) }
    }
}
