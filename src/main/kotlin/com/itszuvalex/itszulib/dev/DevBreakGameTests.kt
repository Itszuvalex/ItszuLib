package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.api.Components
import com.itszuvalex.itszulib.core.BlockEntityCore
import com.itszuvalex.itszulib.verify.BlockEntityContents
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult

/**
 * Game tests for break behaviour with the dev chest ([com.itszuvalex.itszulib.core.BreakBehavior.DROP]) and keeper
 * (`KEEP`): what drops, what the item says it holds, and that placing the item with a player restores it.
 */
object DevBreakGameTests {
    private val AT = BlockPos(2, 1, 2)

    fun register(test: (String, net.minecraft.resources.Identifier, (GameTestHelper) -> Unit) -> Unit) {
        test("keeper_item_shows_what_it_can_hold_and_what_it_holds", DevGameTests.EMPTY_5X3X5, ::tooltip)
        test("keeper_contents_survive_breaking_and_placing_its_item", DevGameTests.EMPTY_5X3X5, ::placing)
        test("chest_drops_its_items_and_keeps_none", DevGameTests.EMPTY_5X3X5, ::chest)
    }

    private fun lines(helper: GameTestHelper, stack: ItemStack): List<String> =
        stack.getTooltipLines(Item.TooltipContext.of(helper.level), null, TooltipFlag.NORMAL).map { it.string }

    private fun dropsAt(helper: GameTestHelper): List<ItemStack> =
        helper.level.getEntitiesOfClass(ItemEntity::class.java, AABB(helper.absolutePos(AT)).inflate(3.0)).map { it.item }

    private fun tooltip(helper: GameTestHelper) {
        val fresh = lines(helper, ItemStack(DevContent.DEV_KEEPER_ITEM.get()))
        helper.assertTrue(fresh.any { "0 / 4 slots" in it }, "a fresh keeper says how many slots it has: $fresh")
        helper.assertTrue(fresh.any { "0 / 4,000 mB" in it }, "a fresh keeper says how much fluid it holds: $fresh")
        helper.assertTrue(fresh.any { "Energy: 0 / 1,000" in it }, "a fresh keeper says how much energy it holds: $fresh")

        helper.setBlock(AT, DevContent.DEV_KEEPER_BLOCK.get())
        BlockEntityContents.fill(helper.getBlockEntity(AT, DevKeeperBlockEntity::class.java))
        helper.level.destroyBlock(helper.absolutePos(AT), true)
        val carried = dropsAt(helper).single { it.has(Components.FRAGMENT_DATA.get()) }
        val full = lines(helper, carried)
        helper.assertTrue(full.any { "4 / 4 slots used" in it }, "a full keeper says its slots are used: $full")
        helper.assertTrue(full.any { "250 / 4,000 mB" in it }, "and its fluid: $full")
        helper.assertTrue(full.any { "Energy: 500 / 1,000" in it }, "and its energy: $full")
        helper.succeed()
    }

    private fun placing(helper: GameTestHelper) {
        helper.setBlock(AT.below(), Blocks.STONE)
        helper.setBlock(AT, DevContent.DEV_KEEPER_BLOCK.get())
        val before = BlockEntityContents.tally(helper.getBlockEntity(AT, DevKeeperBlockEntity::class.java).also { BlockEntityContents.fill(it) })
        helper.level.destroyBlock(helper.absolutePos(AT), true)
        val stacks = dropsAt(helper)
        helper.assertValueEqual(stacks.size, 1, "the keeper drops one item and nothing else, got $stacks")
        helper.assertTrue(stacks[0].has(Components.FRAGMENT_DATA.get()), "which carries its contents")

        val player = helper.makeMockServerPlayerInLevel()
        player.setGameMode(GameType.SURVIVAL)
        val hit = BlockHitResult(helper.absolutePos(AT.below()).center.add(0.0, 0.5, 0.0), Direction.UP, helper.absolutePos(AT.below()), false)
        val result = stacks[0].copy().useOn(UseOnContext(helper.level, player, InteractionHand.MAIN_HAND, stacks[0].copy(), hit))
        helper.assertTrue(result.consumesAction(), "the item places: $result")
        val placed = helper.getBlockEntity(AT, DevKeeperBlockEntity::class.java)
        helper.assertValueEqual(BlockEntityContents.tally(placed as BlockEntityCore), before, "contents after placing the item")
        helper.succeed()
    }

    private fun chest(helper: GameTestHelper) {
        helper.setBlock(AT, DevContent.DEV_CHEST_BLOCK.get())
        val be = helper.getBlockEntity(AT, DevChestBlockEntity::class.java)
        BlockEntityContents.fill(be)
        helper.level.destroyBlock(helper.absolutePos(AT), true)
        val stacks = dropsAt(helper)
        helper.assertTrue(stacks.none { it.has(Components.FRAGMENT_DATA.get()) }, "a chest's item carries nothing")
        helper.assertValueEqual(stacks.filter { !it.`is`(DevContent.DEV_CHEST_ITEM.get()) }.sumOf { it.count }, 1 + 2 + 3 + 4, "its items drop")
        helper.succeed()
    }
}
