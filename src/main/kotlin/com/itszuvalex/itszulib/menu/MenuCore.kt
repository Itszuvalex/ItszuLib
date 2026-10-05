package com.itszuvalex.itszulib.menu

import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.channel.IChannelHost
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.storage.ItemStorageIndex
import com.itszuvalex.itszulib.api.wrappers.WrapperContainerIItemStorage
import com.itszuvalex.itszulib.core.DistributionStatistics
import net.minecraft.core.RegistryAccess
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.transfer.energy.EnergyHandler
import com.itszuvalex.itszulib.api.adapters.IBattery
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
        // A connection that did not negotiate ItszuLib's payloads (a client without it, a test's mock player) gets
        // none; sending one would throw.
        if (!target.connection.hasChannel(MenuSyncPayload.TYPE)) return
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
     * Side configuration support, if [enableSideConfig] was called and the block entity has any of the modes.
     */
    var sideConfig: MenuSideConfig? = null
        private set

    /**
     * Lets this menu's screen edit [blockEntity]'s sided configurations of [modes] (those it has) through
     * [ACTION_SIDE_CONFIG], and those of the other members of its formed multiblock. Call on both sides with the same
     * modes. [onChanged] runs on the server after a change, with the block entity whose configuration changed; by
     * default it saves and syncs it.
     */
    @JvmOverloads
    fun enableSideConfig(
        blockEntity: BlockEntity?,
        modes: List<SideConfigMode> = SideConfigModes.DEFAULTS,
        onChanged: (BlockEntity) -> Unit = MenuSideConfig::markDirtyAndSync,
    ): MenuSideConfig? {
        if (blockEntity == null) return null
        sideConfig = MenuSideConfig(blockEntity, modes, onChanged).takeIf { it.modes.isNotEmpty() }
        return sideConfig
    }

    /**
     * Server side: routes an action from [MenuActionPayload]. ItszuLib's own actions (negative ids, e.g.
     * [ACTION_SIDE_CONFIG]) are handled here; every other action goes to [handleAction].
     */
    fun dispatchAction(player: Player, action: Int, data: Int): Boolean = when {
        action == ACTION_SIDE_CONFIG -> sideConfig?.handle(data) ?: false
        action in StorageTerminal.ACTIONS -> terminal?.handleAction(player, action, data) ?: false
        action in MenuChannels.ACTIONS -> channels?.handle(action, data) ?: false
        action < 0 -> false
        else -> handleAction(player, action, data)
    }

    /**
     * Server side: routes a text action from [MenuTextActionPayload]. ItszuLib's own (negative ids, the channel actions)
     * are handled here; every other action goes to [handleText].
     */
    fun dispatchText(player: Player, action: Int, text: String): Boolean = when {
        action in MenuChannels.TEXT_ACTIONS -> channels?.handleText(action, text) ?: false
        action < 0 -> false
        else -> handleText(player, action, text)
    }

    /**
     * Server side: a control that carries text was used (see [MenuTextActionPayload]). Only called while the menu is
     * open and valid for [player].
     *
     * @return True if handled.
     */
    open fun handleText(player: Player, action: Int, text: String): Boolean = false

    /**
     * Channel support, if [enableChannels] was called.
     */
    var channels: MenuChannels? = null
        private set

    /**
     * Lets this menu's screen join the block behind [host] to channels (see [MenuChannels]): a channel panel opens beside
     * the screen by default ([com.itszuvalex.itszulib.client.screen.ComponentScreen.defaultPanels]). Call on both sides;
     * the client's [host] may return null. The menu's [player] is who joins, creates and deletes.
     */
    fun enableChannels(host: () -> IChannelHost?): MenuChannels {
        val created = MenuChannels(player, host)
        channels = created
        addSync(created.sync)
        return created
    }

    /**
     * The storage terminal, if [enableStorageTerminal] was called.
     */
    var terminal: StorageTerminal? = null
        private set

    /**
     * Gives this menu a view of everything in [index] ([StorageTerminal]) for a searchable, paged screen
     * ([com.itszuvalex.itszulib.client.screen.StorageTerminalView]): items taken out and put in by click, and
     * shift-clicks from the player's inventory put in. Call on both sides (the client's [index] may return null).
     */
    fun enableStorageTerminal(index: () -> ItemStorageIndex?): StorageTerminal {
        val created = StorageTerminal(this, index)
        terminal = created
        addSync(created.viewSync())
        return created
    }

    /**
     * Syncs a battery's energy and capacity into the returned view (the client copy, for screens). [battery] may
     * return null (the view then reads 0).
     */
    fun syncEnergy(battery: () -> IBattery?): EnergyView {
        val view = EnergyView()
        addSync(MenuSyncs.double({ battery()?.storage() ?: 0.0 }, { view.stored = it }))
        addSync(MenuSyncs.double({ battery()?.maxStorage() ?: 0.0 }, { view.capacity = it }))
        return view
    }

    /**
     * Syncs a network's [DistributionStatistics] and its block count ([nodes]): what a network statistics tab shows.
     * [statistics] null means not connected.
     */
    fun syncDistribution(statistics: () -> DistributionStatistics?, nodes: () -> Int = { 0 }): DistributionView {
        val view = DistributionView()
        addSync(MenuSyncs.boolean({ statistics() != null }, { view.connected = it }))
        addSync(MenuSyncs.int({ if (statistics() == null) 0 else nodes() }, { view.nodes = it }))
        addSync(MenuSyncs.int({ statistics()?.producerCount ?: 0 }, { view.producers = it }))
        addSync(MenuSyncs.int({ statistics()?.storageCount ?: 0 }, { view.storage = it }))
        addSync(MenuSyncs.int({ statistics()?.consumerCount ?: 0 }, { view.consumers = it }))
        addSync(MenuSyncs.double({ statistics()?.produced ?: 0.0 }, { view.produced = it }))
        addSync(MenuSyncs.double({ statistics()?.consumed ?: 0.0 }, { view.consumed = it }))
        addSync(MenuSyncs.double({ statistics()?.storageDelta ?: 0.0 }, { view.storageDelta = it }))
        addSync(MenuSyncs.double({ statistics()?.averageTrend ?: 0.0 }, { view.averageTrend = it }))
        addSync(MenuSyncs.double({ statistics()?.dedicatedStored ?: 0.0 }, { view.dedicatedStored = it }))
        addSync(MenuSyncs.double({ statistics()?.dedicatedStorage ?: 0.0 }, { view.dedicatedStorage = it }))
        addSync(MenuSyncs.double({ statistics()?.totalStored ?: 0.0 }, { view.totalStored = it }))
        addSync(MenuSyncs.double({ statistics()?.totalStorage ?: 0.0 }, { view.totalStorage = it }))
        return view
    }

    /**
     * Syncs a NeoForge energy handler's amount and capacity into the returned view, for blocks that use NeoForge's
     * energy API directly.
     */
    fun syncEnergyHandler(handler: () -> EnergyHandler?): EnergyView {
        val view = EnergyView()
        addSync(MenuSyncs.long({ handler()?.amountAsLong ?: 0L }, { view.stored = it.toDouble() }))
        addSync(MenuSyncs.long({ handler()?.capacityAsLong ?: 0L }, { view.capacity = it.toDouble() }))
        return view
    }

    /**
     * Adds [count] slots over [storage] starting at storage index [first], in rows of [columns] from ([x], [y]), 18
     * pixels apart. Must be called before [addPlayerInventorySlots].
     *
     * @param output True for take-only slots (screens ring them).
     * @param hint Shown faded in the slots while empty, to say what goes there.
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
        hint: ItemStack = ItemStack.EMPTY,
    ): IntRange {
        val container = WrapperContainerIItemStorage(storage)
        val start = slots.size
        for (i in 0 until count) {
            val slotX = x + (i % columns) * SLOT_SIZE
            val slotY = y + (i / columns) * SLOT_SIZE
            val slot = if (output) OutputSlot(storage, container, first + i, slotX, slotY) else StorageSlot(storage, container, first + i, slotX, slotY)
            slot.hint = hint
            addBlockSlot(slot)
        }
        return start until slots.size
    }

    /**
     * Adds a [RequirementSlot] for each of [count] indices of [storage] from [first], in rows of [columns] [columnWidth]
     * apart (wider than a slot leaves room for a label beside each): index i collects [required] (i) and gives nothing
     * back.
     */
    @JvmOverloads
    fun addRequirementSlots(
        storage: IItemStorage,
        x: Int,
        y: Int,
        count: Int,
        required: (Int) -> ItemStack,
        columns: Int = 9,
        first: Int = 0,
        columnWidth: Int = SLOT_SIZE,
    ): IntRange {
        val container = WrapperContainerIItemStorage(storage)
        val start = slots.size
        for (i in 0 until count) {
            addBlockSlot(RequirementSlot(storage, container, first + i, x + (i % columns) * columnWidth, y + (i / columns) * SLOT_SIZE) { required(first + i) })
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
        // With a storage terminal, the player's stacks go into the terminal's storage.
        terminal?.let { t -> if (index >= blockSlotCount) return t.quickMove(player, slot) }
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
        // Write the source back rather than relying on the in-place shrink: the slot's storage may have returned a copy.
        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.set(stack)
        if (stack.count == original.count) return ItemStack.EMPTY
        slot.onTake(player, stack)
        return original
    }

    /**
     * Vanilla's merge, except that a slot whose stack grew is written back with [Slot.set] instead of only
     * [Slot.setChanged], so slots over storages that return copies from `get` keep the merged items (REVIEW O1).
     */
    /**
     * Clicking a [RequirementSlot] that already holds some of its item, while carrying more, tops it up (a left click
     * all it takes, a right click one). Vanilla only adds to a slot it may also take from, and requirement slots give
     * nothing back.
     */
    override fun clicked(slotIndex: Int, buttonNum: Int, containerInput: ContainerInput, player: Player) {
        val slot = slots.getOrNull(slotIndex)
        val carried = carried
        if (containerInput == ContainerInput.PICKUP && (buttonNum == 0 || buttonNum == 1) && slot is RequirementSlot &&
            slot.hasItem() && !carried.isEmpty && slot.mayPlace(carried) && ItemStack.isSameItemSameComponents(slot.item, carried)
        ) {
            setCarried(slot.safeInsert(carried, if (buttonNum == 0) carried.count else 1))
            slot.setChanged()
            return
        }
        super.clicked(slotIndex, buttonNum, containerInput, player)
    }

    override fun moveItemStackTo(itemStack: ItemStack, startSlot: Int, endSlot: Int, backwards: Boolean): Boolean {
        var changed = false
        val order = if (backwards) (endSlot - 1 downTo startSlot) else (startSlot until endSlot)
        if (itemStack.isStackable) {
            for (i in order) {
                if (itemStack.isEmpty) break
                val slot = slots[i]
                val target = slot.item
                if (target.isEmpty || !ItemStack.isSameItemSameComponents(itemStack, target)) continue
                val max = slot.getMaxStackSize(target)
                val moved = minOf(itemStack.count, max - target.count)
                if (moved <= 0) continue
                itemStack.shrink(moved)
                slot.set(target.copyWithCount(target.count + moved))
                changed = true
            }
        }
        if (!itemStack.isEmpty) {
            for (i in order) {
                val slot = slots[i]
                if (slot.item.isEmpty && slot.mayPlace(itemStack)) {
                    slot.setByPlayer(itemStack.split(minOf(itemStack.count, slot.getMaxStackSize(itemStack))))
                    slot.setChanged()
                    changed = true
                    break
                }
            }
        }
        return changed
    }

    companion object {
        const val SLOT_SIZE = 18
        const val INVENTORY_SLOTS = 27
        const val HOTBAR_SLOTS = 9

        /**
         * Cycles one face of a sided configuration ([MenuSideConfig.data]); needs [enableSideConfig].
         */
        const val ACTION_SIDE_CONFIG = -1
    }
}

/**
 * Client copy of synced energy (see [MenuCore.syncEnergy] and [MenuCore.syncEnergyHandler]).
 */
/**
 * Client copy of a network's [DistributionStatistics], kept by [MenuCore.syncDistribution].
 */
class DistributionView {
    var connected = false
    var nodes = 0
    var producers = 0
    var storage = 0
    var consumers = 0
    var produced = 0.0
    var consumed = 0.0
    var storageDelta = 0.0
    var averageTrend = 0.0
    var dedicatedStored = 0.0
    var dedicatedStorage = 0.0
    var totalStored = 0.0
    var totalStorage = 0.0
}

class EnergyView {
    var stored = 0.0
    var capacity = 0.0

    val fraction: Double get() = if (capacity <= 0) 0.0 else (stored / capacity).coerceIn(0.0, 1.0)
}

/**
 * How a screen should draw a slot. [isOutput] slots (take-only) get an output ring; while empty, a slot with a
 * [hint] shows it faded, to say what goes there. A slot with a [required] stack counts towards it: the screen shows
 * `have/need` in place of the count (red until met, then green) and drops the slot's inset once it is met.
 */
interface SlotLook {
    val isOutput: Boolean get() = false

    fun hint(): ItemStack = ItemStack.EMPTY

    fun required(): ItemStack = ItemStack.EMPTY
}

/**
 * A slot over one index of an [IItemStorage], honouring its per-slot limit and [IItemStorage.canInsert]. Set [hint]
 * to show what goes in it while it is empty.
 */
open class StorageSlot(@JvmField val storage: IItemStorage, container: Container, index: Int, x: Int, y: Int) : Slot(container, index, x, y), SlotLook {
    /** Shown faded while the slot is empty (client side only matters). */
    @JvmField
    var hint: ItemStack = ItemStack.EMPTY

    override fun hint(): ItemStack = hint

    override fun mayPlace(stack: ItemStack): Boolean = storage.canInsert(containerSlot, IItemStack.of(stack))

    override fun getMaxStackSize(): Int = storage.maxStackSize(containerSlot)

    override fun getMaxStackSize(stack: ItemStack): Int = minOf(storage.maxStackSize(containerSlot), stack.maxStackSize)
}

/**
 * A slot that collects [required] (that item with those data components, up to its count) and gives nothing back:
 * what goes in stays until the block uses it or is broken. Shows the wanted item faded while empty, and `have/need`
 * ([SlotLook.required]). [required] is read each time (it may come from synced state); while it is empty the slot is
 * inactive (hidden, taking nothing).
 */
open class RequirementSlot(storage: IItemStorage, container: Container, index: Int, x: Int, y: Int, private val required: () -> ItemStack) :
    StorageSlot(storage, container, index, x, y) {
    override fun required(): ItemStack = required.invoke()

    override fun hint(): ItemStack = required()

    override fun mayPlace(stack: ItemStack): Boolean {
        val need = required()
        return !need.isEmpty && ItemStack.isSameItemSameComponents(stack, need) && super.mayPlace(stack)
    }

    override fun mayPickup(player: Player): Boolean = false

    override fun isActive(): Boolean = !required().isEmpty

    override fun getMaxStackSize(): Int = minOf(super.getMaxStackSize(), required().count)

    override fun getMaxStackSize(stack: ItemStack): Int = minOf(super.getMaxStackSize(stack), required().count)
}

/**
 * A take-only [StorageSlot], e.g. a machine's output (port of 1.12.2's `OutputSlot`).
 */
open class OutputSlot(storage: IItemStorage, container: Container, index: Int, x: Int, y: Int) : StorageSlot(storage, container, index, x, y) {
    override val isOutput: Boolean get() = true

    override fun mayPlace(stack: ItemStack): Boolean = false
}
