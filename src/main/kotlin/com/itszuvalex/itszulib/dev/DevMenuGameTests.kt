package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IFluidStack
import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.core.HorizontalFacing
import com.itszuvalex.itszulib.menu.MenuActionPayload
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
        test("menu_quick_move", DevGameTests.EMPTY_1, ::menuQuickMove)
        test("multiblock_menu_opens_controller", DevGameTests.EMPTY_5X3X5, ::multiblockMenuOpensController)
        test("horizontal_facing_placement_and_rotation", DevGameTests.EMPTY_1, ::horizontalFacing)
        test("sided_config_follows_facing", DevGameTests.EMPTY_1, ::sidedConfigFollowsFacing)
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
     * A formed multiblock's part opens the controller's menu; an unformed part opens nothing.
     */
    private fun multiblockMenuOpensController(helper: GameTestHelper) {
        helper.setBlock(CENTER, DevContent.DEV_MULTIBLOCK_BLOCK.get())
        helper.setBlock(CENTER.east(), DevContent.DEV_MULTIBLOCK_BLOCK.get())
        val controller = helper.getBlockEntity(CENTER, DevMultiblockBlockEntity::class.java)
        val part = helper.getBlockEntity(CENTER.east(), DevMultiblockBlockEntity::class.java)
        helper.assertTrue(part.getModule(Modules.MENU, null) == null, "unformed part has a menu")

        DevMultiblockBlock.MULTIBLOCK.tryForm(helper.level, helper.absolutePos(CENTER), DevMultiblockBlock.PATTERN)
        val menu = part.getModule(Modules.MENU, Direction.UP)
        helper.assertTrue(menu != null, "formed part has no menu")
        helper.assertValueEqual(menu!!.menuPos(), controller.blockPos, "menu position")
        val created = menu.createMenu(3, helper.makeMockPlayer(GameType.SURVIVAL).inventory, helper.makeMockPlayer(GameType.SURVIVAL))
        helper.assertTrue((created as DevMenu).blockEntity === controller, "menu is not over the controller")
        helper.succeed()
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
}
