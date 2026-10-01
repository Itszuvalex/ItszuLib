package com.itszuvalex.itszulib.menu

import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.WrapperContainerIItemStorage
import net.minecraft.core.RegistryAccess
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.network.PacketDistributor

/**
 * Base menu: the block's slots first ([addStorageSlots]), then the player's inventory ([addPlayerInventorySlots]),
 * shift-click between them, and [MenuSync]s for values that are not slots. Port of ItszuLib 1.12.2's
 * `ContainerBase`/`ContainerInv`. Slots stay vanilla [Slot]s (DECISIONS D6).
 *
 * @param player The player the menu is open for; server-side syncs are sent to it. Null only in tests.
 */
abstract class MenuCore(type: MenuType<*>?, containerId: Int, @JvmField val player: Player?) : AbstractContainerMenu(type, containerId) {
    private val syncs = ArrayList<MenuSync<*>>()

    /**
     * Number of slots added before the player's inventory: the block's slots.
     */
    var blockSlotCount = 0
        private set

    private var playerSlotsAdded = false

    fun <T : Any> addSync(sync: MenuSync<T>): MenuSync<T> {
        sync.index = syncs.size
        syncs += sync
        return sync
    }

    fun syncs(): List<MenuSync<*>> = syncs

    /**
     * Server side: a payload with every sync that changed since the last call, or every sync if [all].
     *
     * @return Null if there is nothing to send.
     */
    fun collectSyncPayload(registries: RegistryAccess, all: Boolean): MenuSyncPayload? {
        val entries = ArrayList<MenuSyncPayload.Entry>()
        for (sync in syncs) {
            val changed = sync.poll()
            if (!changed && !all) continue
            val buf = MenuSyncPayload.buffer(registries)
            try {
                sync.encode(buf)
                val bytes = ByteArray(buf.readableBytes())
                buf.readBytes(bytes)
                entries += MenuSyncPayload.Entry(sync.index, bytes)
            } finally {
                buf.release()
            }
        }
        return if (entries.isEmpty()) null else MenuSyncPayload(containerId, entries)
    }

    /**
     * Client side: applies [payload] if it is for this menu. Unknown sync indices are ignored.
     *
     * @return False if the payload is for another menu.
     */
    fun applySyncPayload(payload: MenuSyncPayload, registries: RegistryAccess): Boolean {
        if (payload.containerId != containerId) return false
        for (entry in payload.entries) {
            val sync = syncs.getOrNull(entry.index) ?: continue
            val buf = MenuSyncPayload.buffer(registries, entry.data)
            try {
                sync.decode(buf)
            } finally {
                buf.release()
            }
        }
        return true
    }

    override fun broadcastChanges() {
        super.broadcastChanges()
        sendSyncs(false)
    }

    override fun broadcastFullState() {
        super.broadcastFullState()
        sendSyncs(true)
    }

    override fun sendAllDataToRemote() {
        super.sendAllDataToRemote()
        sendSyncs(true)
    }

    private fun sendSyncs(all: Boolean) {
        val target = player as? ServerPlayer ?: return
        collectSyncPayload(target.registryAccess(), all)?.let { PacketDistributor.sendToPlayer(target, it) }
    }

    /**
     * Server side: a control in this menu's screen was used (see [MenuActionPayload]). Only called while the menu is
     * open and valid for [player].
     *
     * @return True if handled.
     */
    open fun handleAction(player: Player, action: Int, data: Int): Boolean = false

    /**
     * Adds [count] slots over [storage] starting at storage index [first], in rows of [columns] from ([x], [y]), 18
     * pixels apart. Must be called before [addPlayerInventorySlots].
     *
     * @param output True for take-only slots.
     */
    @JvmOverloads
    fun addStorageSlots(
        storage: IItemStorage,
        x: Int,
        y: Int,
        columns: Int = 9,
        first: Int = 0,
        count: Int = storage.size() - first,
        output: Boolean = false,
    ): IntRange {
        val container = WrapperContainerIItemStorage(storage)
        val start = slots.size
        for (i in 0 until count) {
            val slotX = x + (i % columns) * SLOT_SIZE
            val slotY = y + (i / columns) * SLOT_SIZE
            addBlockSlot(if (output) OutputSlot(storage, container, first + i, slotX, slotY) else StorageSlot(storage, container, first + i, slotX, slotY))
        }
        return start until slots.size
    }

    /**
     * Adds a slot belonging to the block. Must be called before [addPlayerInventorySlots].
     */
    fun addBlockSlot(slot: Slot): Slot {
        check(!playerSlotsAdded) { "Add the block's slots before the player's inventory" }
        addSlot(slot)
        blockSlotCount++
        return slot
    }

    /**
     * Adds the player's 27 inventory slots at ([x], [y]) and the hotbar 58 pixels below, as vanilla screens lay them
     * out (default (8, 84) for a 166-pixel-tall screen).
     */
    @JvmOverloads
    fun addPlayerInventorySlots(inventory: Inventory, x: Int = 8, y: Int = 84) {
        check(!playerSlotsAdded) { "Player inventory slots already added" }
        playerSlotsAdded = true
        for (row in 0 until 3) {
            for (col in 0 until 9) addSlot(Slot(inventory, col + row * 9 + 9, x + col * SLOT_SIZE, y + row * SLOT_SIZE))
        }
        for (col in 0 until 9) addSlot(Slot(inventory, col, x + col * SLOT_SIZE, y + 58))
    }

    /**
     * Shift-click: block slots go to the player's inventory (hotbar first, as vanilla does); player slots go to the
     * first block slots that accept them, otherwise between the main inventory and the hotbar. 1.12.2 gave up when an
     * item it considered input did not fit, instead of falling back to the inventory/hotbar swap.
     */
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val original = stack.copy()
        val playerStart = blockSlotCount
        val hotbarStart = playerStart + INVENTORY_SLOTS
        val end = slots.size
        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, end, true)) return ItemStack.EMPTY
        } else if (!moveItemStackTo(stack, 0, playerStart, false)) {
            val moved = if (index < hotbarStart) moveItemStackTo(stack, hotbarStart, end, false) else moveItemStackTo(stack, playerStart, hotbarStart, false)
            if (!moved) return ItemStack.EMPTY
        }
        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        if (stack.count == original.count) return ItemStack.EMPTY
        slot.onTake(player, stack)
        return original
    }

    companion object {
        const val SLOT_SIZE = 18
        const val INVENTORY_SLOTS = 27
        const val HOTBAR_SLOTS = 9
    }
}

/**
 * A slot over one index of an [IItemStorage], honouring its per-slot limit and [IItemStorage.canInsert].
 */
open class StorageSlot(@JvmField val storage: IItemStorage, container: Container, index: Int, x: Int, y: Int) : Slot(container, index, x, y) {
    override fun mayPlace(stack: ItemStack): Boolean = storage.canInsert(containerSlot, IItemStack.of(stack))

    override fun getMaxStackSize(): Int = storage.maxStackSize(containerSlot)

    override fun getMaxStackSize(stack: ItemStack): Int = minOf(storage.maxStackSize(containerSlot), stack.maxStackSize)
}

/**
 * A take-only [StorageSlot], e.g. a machine's output (port of 1.12.2's `OutputSlot`).
 */
open class OutputSlot(storage: IItemStorage, container: Container, index: Int, x: Int, y: Int) : StorageSlot(storage, container, index, x, y) {
    override fun mayPlace(stack: ItemStack): Boolean = false
}
