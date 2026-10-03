package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IFluidStack
import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.core.HorizontalFacing
import com.itszuvalex.itszulib.api.storage.ItemStorageArray
import com.itszuvalex.itszulib.api.storage.ItemStorageNBT
import net.minecraft.nbt.CompoundTag
import com.itszuvalex.itszulib.menu.MenuActionPayload
import com.itszuvalex.itszulib.menu.MenuCore
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.Identifier
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.capabilities.Capabilities as NeoCapabilities

/**
 * Game tests for menus and syncs (F5-F7), horizontal facing (F8) and wire networks (F9).
 */
object DevMenuGameTests {
    private val CENTER = BlockPos(2, 1, 2)

    fun register(test: (String, Identifier, (GameTestHelper) -> Unit) -> Unit) {
        test("menu_opens_on_use", DevGameTests.EMPTY_1, ::menuOpensOnUse)
        test("menu_syncs_to_client_menu", DevGameTests.EMPTY_1, ::menuSyncsToClientMenu)
        test("menu_action_dispatch", DevGameTests.EMPTY_1, ::menuActionDispatch)
        test("menu_side_config_action", DevGameTests.EMPTY_1, ::menuSideConfigAction)
        test("menu_quick_move", DevGameTests.EMPTY_1, ::menuQuickMove)
        test("menu_storage_slots_honour_can_insert", DevGameTests.EMPTY_1, ::menuSlotsHonourCanInsert)
        test("menu_slots_over_copy_returning_storage", DevGameTests.EMPTY_1, ::menuOverCopyReturningStorage)
        test("multiblock_menu_opens_on_any_part", DevGameTests.EMPTY_5X3X5, ::multiblockMenuOpensOnAnyPart)
        test("multiblock_side_config_reaches_every_member", DevGameTests.EMPTY_5X3X5, ::multiblockSideConfig)
        test("horizontal_facing_placement_and_rotation", DevGameTests.EMPTY_1, ::horizontalFacing)
        test("sided_config_follows_facing", DevGameTests.EMPTY_1, ::sidedConfigFollowsFacing)
        test("storage_terminal_lists_takes_and_puts", DevGameTests.EMPTY_1, ::storageTerminal)
        test("wire_network_forms_and_splits", DevGameTests.EMPTY_5X3X5, ::wireNetwork)
    }

    private fun machine(helper: GameTestHelper, pos: BlockPos = BlockPos.ZERO): DevMachineBlockEntity {
        helper.setBlock(pos, DevContent.DEV_MACHINE_BLOCK.get())
        return helper.getBlockEntity(pos, DevMachineBlockEntity::class.java)
    }

    /**
     * Using the block finds its menu through EntityBlockCore and the MENU module. (A mock server player cannot receive
     * the open-screen payload, so the menu is created directly from the host.)
     */
    private fun menuOpensOnUse(helper: GameTestHelper) {
        val be = machine(helper)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        player.setPos(Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO).above()))
        val pos = helper.absolutePos(BlockPos.ZERO)
        val hit = BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
        val result = helper.level.getBlockState(pos).useWithoutItem(helper.level, player, hit)
        helper.assertTrue(result.consumesAction(), "use was not consumed: $result")

        val host = be.getModule(Modules.MENU, Direction.UP)!!
        helper.assertValueEqual(host.menuPos(), pos, "menu position")
        val menu = host.createMenu(1, player.inventory, player)
        helper.assertTrue(menu is DevMenu && menu.blockEntity === be, "opened $menu")
        helper.assertTrue(menu!!.stillValid(player), "menu not valid next to the block")
        helper.succeed()
    }

    /**
     * Server menu values reach a client copy of the menu (over a client copy of the block entity).
     */
    private fun menuSyncsToClientMenu(helper: GameTestHelper) {
        val be = machine(helper)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val registries = helper.level.registryAccess()
        val server = DevMenu(5, player.inventory, be)
        val clientBe = DevMachineBlockEntity(be.blockPos, be.blockState)
        val client = DevMenu(5, player.inventory, clientBe)

        be.tanks.set(0, IFluidStack.of(FluidStack(Fluids.WATER, 1500)))
        client.applySyncPayload(server.collectSyncPayload(registries, all = false)!!, registries)
        helper.assertValueEqual(clientBe.tanks.get(0).amount(), 1500, "synced amount")
        helper.assertTrue(clientBe.tanks.get(0).toMinecraft().`is`(Fluids.WATER), "synced fluid")

        helper.assertTrue(server.collectSyncPayload(registries, all = false) == null, "unchanged values were resent")
        be.tanks.get(0).setAmount(700) // changed in place
        client.applySyncPayload(server.collectSyncPayload(registries, all = false)!!, registries)
        helper.assertValueEqual(clientBe.tanks.get(0).amount(), 700, "in-place change synced")
        helper.succeed()
    }

    /**
     * Actions reach only the open menu they name, and only while the player can use it.
     */
    private fun menuActionDispatch(helper: GameTestHelper) {
        val be = machine(helper)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        player.setPos(Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO).above()))
        val menu = DevMenu(9, player.inventory, be)
        val east = Direction.EAST.ordinal
        val config = be.itemConfig.configuration

        helper.assertFalse(MenuActionPayload.dispatch(menu, MenuActionPayload(8, DevMenu.ACTION_CYCLE_STORAGE, east), player), "other container id")
        helper.assertTrue(MenuActionPayload.dispatch(menu, MenuActionPayload(9, DevMenu.ACTION_CYCLE_STORAGE, east), player), "matching action")
        helper.assertValueEqual(config.getStorageNameForRelativeFacing(Direction.EAST), "output", "cycled storage")
        helper.assertFalse(MenuActionPayload.dispatch(menu, MenuActionPayload(9, DevMenu.ACTION_CYCLE_STORAGE, 42), player), "bad face")

        player.setPos(player.position().add(100.0, 0.0, 0.0))
        helper.assertFalse(MenuActionPayload.dispatch(menu, MenuActionPayload(9, DevMenu.ACTION_CYCLE_STORAGE, east), player), "out of reach")
        helper.assertValueEqual(config.getStorageNameForRelativeFacing(Direction.EAST), "output", "out-of-reach action applied")
        helper.succeed()
    }

    /**
     * [MenuCore.ACTION_SIDE_CONFIG] cycles the face of the chosen mode's configuration; the menu offers only the modes
     * the block entity has; bad faces, modes and ItszuLib action ids are refused.
     */
    private fun menuSideConfigAction(helper: GameTestHelper) {
        val be = machine(helper)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        player.setPos(Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO).above()))
        val menu = DevMenu(9, player.inventory, be)
        val side = menu.sideConfig!!
        helper.assertValueEqual(side.modes, com.itszuvalex.itszulib.menu.SideConfigModes.DEFAULTS, "modes")
        fun act(data: Int) = MenuActionPayload.dispatch(menu, MenuActionPayload(9, com.itszuvalex.itszulib.menu.MenuCore.ACTION_SIDE_CONFIG, data), player)
        val energyMode = side.modes.indexOf(com.itszuvalex.itszulib.menu.SideConfigModes.ENERGY)
        val face = Direction.UP
        helper.assertTrue(act(com.itszuvalex.itszulib.menu.MenuSideConfig.data(face, energyMode, false)), "handled")
        helper.assertValueEqual(be.energyConfig.configuration.getIOForAbsoluteFacing(face), com.itszuvalex.itszulib.core.EnumAutomaticIO.INPUT, "energy face cycled")
        helper.assertValueEqual(be.itemConfig.configuration.getIOForAbsoluteFacing(face), com.itszuvalex.itszulib.core.EnumAutomaticIO.NONE, "item face untouched")
        act(com.itszuvalex.itszulib.menu.MenuSideConfig.data(face, energyMode, true))
        helper.assertValueEqual(be.energyConfig.configuration.getIOForAbsoluteFacing(face), com.itszuvalex.itszulib.core.EnumAutomaticIO.NONE, "cycled back")
        helper.assertFalse(act(7), "bad face")
        helper.assertFalse(act(com.itszuvalex.itszulib.menu.MenuSideConfig.data(face, 9, false)), "bad mode")
        helper.assertFalse(MenuActionPayload.dispatch(menu, MenuActionPayload(9, -2, 0), player), "unknown ItszuLib action")

        val multi = helper.makeMockPlayer(GameType.SURVIVAL)
        helper.assertTrue(DevMenu(10, multi.inventory, null).sideConfig == null, "no block entity, no side config")
        helper.succeed()
    }

    /**
     * Shift-click: player items go to the input slot (never the output slot); block items go to the player; when the
     * block cannot take an item it moves between main inventory and hotbar instead.
     */
    private fun menuQuickMove(helper: GameTestHelper) {
        val be = machine(helper)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val menu = DevMenu(1, player.inventory, be)
        val mainStart = menu.blockSlotCount
        val hotbarStart = mainStart + 27

        player.inventory.setItem(9, ItemStack(Items.DIAMOND, 5)) // first main-inventory slot
        menu.quickMoveStack(player, mainStart)
        helper.assertValueEqual(be.inventory.get(0).stackSize(), 5, "moved into the input slot")
        helper.assertTrue(be.inventory.get(1).isEmpty(), "moved into the output slot")

        be.inventory.setSlot(1, IItemStack.of(ItemStack(Items.GOLD_INGOT, 3)))
        menu.quickMoveStack(player, 1)
        helper.assertTrue(be.inventory.get(1).isEmpty(), "output not taken")
        helper.assertValueEqual(player.inventory.countItem(Items.GOLD_INGOT), 3, "gold in the player inventory")

        // Regression: with the input full, 1.12.2 gave up instead of moving the stack to the hotbar.
        be.inventory.setSlot(0, IItemStack.of(ItemStack(Items.DIRT, 64)))
        player.inventory.setItem(10, ItemStack(Items.EMERALD, 2))
        menu.quickMoveStack(player, mainStart + 1)
        val hotbar = (0 until 9).map { player.inventory.getItem(it) }
        helper.assertTrue(hotbar.any { it.`is`(Items.EMERALD) && it.count == 2 }, "emeralds not moved to the hotbar: $hotbar")
        helper.assertTrue(menu.slots[hotbarStart].container === player.inventory, "hotbar slots follow the main inventory")
        helper.succeed()
    }

    /**
     * Regression: `StorageSlot` did not override `Slot.mayPlace` (always true), so clicks, hotbar swaps and shift-clicks
     * put items into slots whose storage refused them.
     */
    private fun menuSlotsHonourCanInsert(helper: GameTestHelper) {
        val storage = object : ItemStorageArray(2) {
            override fun canInsert(index: Int, stack: IItemStack): Boolean = !stack.toMinecraft().`is`(Items.DIAMOND)
        }
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val menu = object : MenuCore(DevContent.DEV_MENU.get(), 1, player) {
            init {
                addStorageSlots(storage, 0, 0)
                addPlayerInventorySlots(player.inventory)
            }

            override fun stillValid(player: Player): Boolean = true
        }
        helper.assertTrue(!menu.slots[0].mayPlace(ItemStack(Items.DIAMOND)), "slot refuses what the storage refuses")
        helper.assertTrue(menu.slots[0].mayPlace(ItemStack(Items.EMERALD)), "slot takes what the storage takes")

        player.inventory.setItem(0, ItemStack(Items.DIAMOND, 3))
        menu.clicked(0, 0, ContainerInput.SWAP, player)
        helper.assertTrue(storage.get(0).isEmpty(), "hotbar swap refused")
        menu.quickMoveStack(player, menu.blockSlotCount + 27)
        helper.assertTrue(storage.get(0).isEmpty() && storage.get(1).isEmpty(), "shift-click refused")
        menu.setCarried(ItemStack(Items.DIAMOND))
        menu.clicked(1, 0, ContainerInput.PICKUP, player)
        helper.assertTrue(storage.get(1).isEmpty(), "click refused")
        helper.assertValueEqual(player.inventory.countItem(Items.DIAMOND) + menu.carried.count, 4, "no diamonds lost")
        helper.succeed()
    }

    /**
     * Regression (REVIEW O1): shift-click changed slot stacks in place and only called `setChanged`. Behind a storage
     * whose `get` returns a copy (`ItemStorageNBT`), merging into a slot lost the items and moving out of one
     * duplicated them. Every click type must conserve items over such a storage.
     */
    private fun menuOverCopyReturningStorage(helper: GameTestHelper) {
        val storage = ItemStorageNBT(CompoundTag(), 3, helper.level.registryAccess())
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val menu = object : MenuCore(DevContent.DEV_MENU.get(), 1, player) {
            init {
                addStorageSlots(storage, 0, 0)
                addPlayerInventorySlots(player.inventory)
            }

            override fun stillValid(player: Player): Boolean = true
        }
        val mainStart = menu.blockSlotCount
        fun total() = (0 until storage.size()).sumOf { storage.get(it).stackSize() } + player.inventory.countItem(Items.DIRT) + menu.carried.count

        storage.setSlot(0, IItemStack.of(ItemStack(Items.DIRT, 10)))
        player.inventory.setItem(9, ItemStack(Items.DIRT, 5))
        menu.quickMoveStack(player, mainStart)
        helper.assertValueEqual(storage.get(0).stackSize(), 15, "shift-click merged into the storage slot")
        helper.assertValueEqual(total(), 15, "nothing lost merging")

        menu.quickMoveStack(player, 0)
        helper.assertTrue(storage.get(0).isEmpty(), "shift-click out emptied the storage slot")
        helper.assertValueEqual(total(), 15, "nothing duplicated moving out")

        // Partial move out: only 4 fit in the player's inventory, so 11 must stay in the slot.
        storage.setSlot(0, IItemStack.of(ItemStack(Items.DIRT, 15)))
        for (i in 0 until 36) player.inventory.setItem(i, ItemStack(Items.STONE))
        player.inventory.setItem(9, ItemStack(Items.DIRT, 60))
        menu.quickMoveStack(player, 0)
        helper.assertValueEqual(storage.get(0).stackSize(), 11, "the rest stays in the storage slot")
        helper.assertValueEqual(player.inventory.countItem(Items.DIRT), 64, "four moved")
        for (i in 0 until 36) player.inventory.setItem(i, ItemStack.EMPTY)
        storage.setSlot(0, IItemStack.Empty)

        // Plain clicks: take all, place one, place the rest onto it, take half, swap with the hotbar, collect all.
        storage.setSlot(1, IItemStack.of(ItemStack(Items.DIRT, 8)))
        menu.clicked(1, 0, ContainerInput.PICKUP, player)
        menu.clicked(2, 1, ContainerInput.PICKUP, player)
        menu.clicked(2, 0, ContainerInput.PICKUP, player)
        helper.assertValueEqual(storage.get(2).stackSize(), 8, "placed onto the same item")
        menu.clicked(2, 1, ContainerInput.PICKUP, player)
        helper.assertValueEqual(storage.get(2).stackSize(), 4, "took half")
        menu.clicked(1, 0, ContainerInput.PICKUP, player)
        menu.clicked(1, 0, ContainerInput.SWAP, player)
        menu.clicked(2, 0, ContainerInput.PICKUP_ALL, player)
        helper.assertValueEqual(total(), 8, "clicks conserve items")
        helper.succeed()
    }

    /**
     * A part of a formed structure opens its own menu, over the structure's shared state; an unformed part opens
     * nothing.
     */
    private fun multiblockMenuOpensOnAnyPart(helper: GameTestHelper) {
        helper.setBlock(CENTER, DevContent.DEV_MULTIBLOCK_BLOCK.get())
        val core = helper.getBlockEntity(CENTER, DevMultiblockBlockEntity::class.java)
        helper.runAfterDelay(2) {
            helper.assertTrue(core.getModule(Modules.MENU, null) == null, "unformed part has a menu")
            helper.setBlock(CENTER.east(), DevContent.DEV_MULTIBLOCK_BLOCK.get())
            val wing = helper.getBlockEntity(CENTER.east(), DevMultiblockBlockEntity::class.java)
            helper.runAfterDelay(2) {
                val menu = wing.getModule(Modules.MENU, Direction.UP)
                helper.assertTrue(menu != null, "formed part has no menu")
                helper.assertValueEqual(menu!!.menuPos(), wing.blockPos, "menu position")
                val player = helper.makeMockPlayer(GameType.SURVIVAL)
                val created = menu.createMenu(3, player.inventory, player) as DevMenu
                helper.assertTrue(created.blockEntity === wing, "menu is not over the part")
                helper.assertTrue(wing.counter() === core.counter(), "the part's menu does not read the shared state")
                helper.succeed()
            }
        }
    }

    /**
     * A formed member's menu configures every member of its structure: the action's member offset picks the member,
     * faces between members stay locked, and offsets outside the structure are refused.
     */
    private fun multiblockSideConfig(helper: GameTestHelper) {
        helper.setBlock(CENTER, DevContent.DEV_MULTIBLOCK_BLOCK.get())
        helper.setBlock(CENTER.east(), DevContent.DEV_MULTIBLOCK_BLOCK.get())
        val core = helper.getBlockEntity(CENTER, DevMultiblockBlockEntity::class.java)
        val wing = helper.getBlockEntity(CENTER.east(), DevMultiblockBlockEntity::class.java)
        helper.runAfterDelay(2) {
            val player = helper.makeMockPlayer(GameType.SURVIVAL)
            player.setPos(Vec3.atCenterOf(helper.absolutePos(CENTER).above()))
            val menu = DevMenu(11, player.inventory, core)
            val side = menu.sideConfig!!
            helper.assertValueEqual(side.members(), listOf(core.blockPos, wing.blockPos), "members")
            fun act(face: Direction, member: BlockPos) =
                MenuActionPayload.dispatch(menu, MenuActionPayload(11, MenuCore.ACTION_SIDE_CONFIG, com.itszuvalex.itszulib.menu.MenuSideConfig.data(face, 0, false, member)), player)
            val wingConfig = wing.itemConfig.configuration
            val coreConfig = core.itemConfig.configuration
            helper.assertTrue(act(Direction.EAST, BlockPos(1, 0, 0)), "wing's outer face handled")
            helper.assertValueEqual(wingConfig.getIOForAbsoluteFacing(Direction.EAST), com.itszuvalex.itszulib.core.EnumAutomaticIO.INPUT, "wing's outer face cycled")
            helper.assertValueEqual(coreConfig.getIOForAbsoluteFacing(Direction.EAST), com.itszuvalex.itszulib.core.EnumAutomaticIO.NONE, "core's inner face untouched")
            act(Direction.WEST, BlockPos(1, 0, 0))
            helper.assertValueEqual(wingConfig.getIOForAbsoluteFacing(Direction.WEST), com.itszuvalex.itszulib.core.EnumAutomaticIO.NONE, "inner face stays locked")
            helper.assertFalse(act(Direction.UP, BlockPos(0, 1, 0)), "offset outside the structure")
            helper.assertFalse(act(Direction.UP, BlockPos(-1, 0, 0)), "offset to a non-member")

            helper.destroyBlock(CENTER.east())
            helper.assertValueEqual(side.members(), listOf(core.blockPos), "members after the structure broke")
            helper.assertFalse(act(Direction.EAST, BlockPos(1, 0, 0)), "former member")
            helper.succeed()
        }
    }

    /**
     * Placed facing the player; rotation and mirroring turn the front.
     */
    private fun horizontalFacing(helper: GameTestHelper) {
        val block = DevContent.DEV_MACHINE_BLOCK.get()
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        player.setYRot(-90f) // looking east
        val pos = helper.absolutePos(BlockPos.ZERO)
        val context = BlockPlaceContext(player, InteractionHand.MAIN_HAND, ItemStack.EMPTY, BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false))
        val placed = block.getStateForPlacement(context)!!
        helper.assertValueEqual(placed.getValue(HorizontalFacing.FACING), Direction.WEST, "front faces the player")
        helper.assertValueEqual(placed.rotate(Rotation.CLOCKWISE_90).getValue(HorizontalFacing.FACING), Direction.NORTH, "rotated")
        helper.assertValueEqual(placed.mirror(Mirror.FRONT_BACK).getValue(HorizontalFacing.FACING), Direction.EAST, "mirrored")
        helper.succeed()
    }

    /**
     * Side configuration is relative to the front: on a machine facing east, the relative north face is the world east
     * face.
     */
    private fun sidedConfigFollowsFacing(helper: GameTestHelper) {
        helper.setBlock(BlockPos.ZERO, DevContent.DEV_MACHINE_BLOCK.get().defaultBlockState().setValue(HorizontalFacing.FACING, Direction.EAST))
        val be = helper.getBlockEntity(BlockPos.ZERO, DevMachineBlockEntity::class.java)
        be.itemConfig.update { it.cycleRelativeFacingStorageForward(Direction.NORTH) }
        val pos = helper.absolutePos(BlockPos.ZERO)
        helper.assertTrue(be.getModule(Modules.ITEM_STORAGE, Direction.EAST) === be.output, "world east face is the configured front")
        helper.assertTrue(be.getModule(Modules.ITEM_STORAGE, Direction.NORTH) === be.input, "world north face kept the default")
        helper.assertTrue(helper.level.getCapability(NeoCapabilities.Item.BLOCK, pos, Direction.EAST) != null, "capability missing")
        helper.succeed()
    }

    /**
     * Wires placed in a line join one network once loaded; breaking the middle one splits it and clears the
     * neighbours' connection flags.
     */
    private fun wireNetwork(helper: GameTestHelper) {
        val positions = listOf(CENTER.west(), CENTER, CENTER.east())
        positions.forEach { helper.setBlock(it, DevContent.DEV_WIRE_BLOCK.get()) }
        val (west, middle, east) = positions.map { helper.getBlockEntity(it, DevWireBlockEntity::class.java).wire }
        helper.runAfterDelay(2) {
            helper.assertTrue(west.getNetwork() != null && west.getNetwork() === east.getNetwork(), "not one network")
            helper.assertValueEqual(west.getNetwork()!!.size(), 3, "network size")
            helper.assertTrue(middle.isConnected(Direction.WEST) && middle.isConnected(Direction.EAST), "middle connections")

            helper.destroyBlock(CENTER)
            helper.assertFalse(west.isConnected(Direction.EAST), "west still connected to the broken wire")
            helper.assertFalse(east.isConnected(Direction.WEST), "east still connected to the broken wire")
            helper.assertTrue(west.getNetwork() !== east.getNetwork(), "network did not split")
            helper.assertValueEqual(west.getNetwork()?.size() ?: -1, 1, "west network size")
            helper.succeed()
        }
    }

    /** A menu with only a storage terminal and the player's inventory. */
    private class TerminalMenu(id: Int, player: Player, index: com.itszuvalex.itszulib.api.storage.ItemStorageIndex?) : MenuCore(null, id, player) {
        val view = enableStorageTerminal { index }

        init {
            addPlayerInventorySlots(player.inventory)
        }

        override fun stillValid(player: Player): Boolean = true
    }

    /**
     * The terminal lists each kind of stack across its storages with its count and syncs that list; extract takes a
     * stack to the carried slot (or half, or into the inventory), insert puts the carried stack (or one) back, and
     * shift-clicking a player slot puts its stack in. Stacks with other components are separate entries.
     */
    private fun storageTerminal(helper: GameTestHelper) {
        val a = com.itszuvalex.itszulib.api.storage.IndexedItemStorage(ItemStorageArray(9))
        val b = com.itszuvalex.itszulib.api.storage.IndexedItemStorage(ItemStorageArray(9))
        val index = com.itszuvalex.itszulib.api.storage.ItemStorageIndex().apply { add(a); add(b) }
        a.insert(IItemStack.of(ItemStack(Items.IRON_INGOT, 64)))
        b.insert(IItemStack.of(ItemStack(Items.IRON_INGOT, 36)))
        b.insert(IItemStack.of(ItemStack(Items.DIAMOND, 3)))
        val named = ItemStack(Items.DIAMOND).apply { set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Shiny")) }
        a.insert(IItemStack.of(named))

        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val registries = helper.level.registryAccess()
        val server = TerminalMenu(4, player, index)
        val client = TerminalMenu(4, player, null)
        client.applySyncPayload(server.collectSyncPayload(registries, all = false)!!, registries)
        fun count(menu: TerminalMenu, item: net.minecraft.world.item.Item, custom: Boolean = false) =
            menu.view.stacks.filter { it.stack.`is`(item) && it.stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) == custom }.sumOf { it.count }
        helper.assertValueEqual(client.view.stacks.size, 3, "entries")
        helper.assertValueEqual(count(client, Items.IRON_INGOT), 100L, "iron across storages")
        helper.assertValueEqual(count(client, Items.DIAMOND), 3L, "plain diamonds")
        helper.assertValueEqual(count(client, Items.DIAMOND, custom = true), 1L, "named diamond kept apart")

        fun entry(item: net.minecraft.world.item.Item, custom: Boolean = false) =
            server.view.stacks.indexOfFirst { it.stack.`is`(item) && it.stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) == custom }
        fun act(action: Int, data: Int) = MenuActionPayload.dispatch(server, MenuActionPayload(4, action, data), player)
        val T = com.itszuvalex.itszulib.menu.StorageTerminal

        helper.assertTrue(act(T.ACTION_EXTRACT, T.extractData(entry(Items.IRON_INGOT), false, false)), "extract")
        helper.assertValueEqual(server.carried.count, 64, "a whole stack carried")
        helper.assertValueEqual(index.count(IItemStack.of(ItemStack(Items.IRON_INGOT)).item()), 36L, "left in storage")
        helper.assertFalse(act(T.ACTION_EXTRACT, T.extractData(entry(Items.DIAMOND), false, false)), "no extract onto a carried stack")
        helper.assertTrue(act(T.ACTION_INSERT, T.INSERT_ONE), "insert one")
        helper.assertValueEqual(server.carried.count, 63, "one put back")
        helper.assertTrue(act(T.ACTION_INSERT, 0), "insert all")
        helper.assertTrue(server.carried.isEmpty, "all put back")

        helper.assertTrue(server.collectSyncPayload(registries, all = false) == null, "a round trip that changed nothing was resent")

        helper.assertTrue(act(T.ACTION_EXTRACT, T.extractData(entry(Items.DIAMOND), false, true)), "extract half")
        helper.assertValueEqual(server.carried.count, 2, "half of three, rounded up")
        helper.assertFalse(server.carried.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME), "the plain diamonds, not the named one")
        client.applySyncPayload(server.collectSyncPayload(registries, all = false) ?: error("no resync after a change"), registries)
        helper.assertValueEqual(count(client, Items.DIAMOND), 1L, "resynced count")
        server.setCarried(ItemStack.EMPTY)
        helper.assertTrue(act(T.ACTION_EXTRACT, T.extractData(entry(Items.DIAMOND, custom = true), true, false)), "extract to inventory")
        helper.assertTrue(player.inventory.contains { it.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) }, "named diamond in the inventory")
        helper.assertFalse(act(T.ACTION_EXTRACT, T.extractData(99, false, false)), "unknown entry")

        val slot = server.slots.indexOfFirst { it.item.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) }
        server.quickMoveStack(player, slot)
        helper.assertTrue(server.slots[slot].item.isEmpty, "shift-click emptied the slot")
        server.collectSyncPayload(registries, all = false)
        helper.assertValueEqual(count(server, Items.DIAMOND, custom = true), 1L, "shift-click put it in")
        helper.succeed()
    }
}
