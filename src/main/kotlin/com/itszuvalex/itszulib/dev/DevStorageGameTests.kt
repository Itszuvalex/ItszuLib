package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IFluidStack
import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.storage.FluidStorageArray
import com.itszuvalex.itszulib.api.wrappers.WrapperResourceHandlerIFluidStorage
import com.itszuvalex.itszulib.core.EnumAutomaticIO
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.Identifier
import net.minecraft.util.ProblemReporter
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.storage.TagValueInput
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.item.ItemResource
import net.neoforged.neoforge.transfer.transaction.Transaction
import net.neoforged.neoforge.capabilities.Capabilities as NeoCapabilities

/**
 * Game tests for fluid storage, storage fragments, sided configuration, automatic IO and multiblocks (1.12.2 features
 * F1-F4), on [DevMachineBlockEntity] and [DevMultiblockBlockEntity].
 */
object DevStorageGameTests {
    /**
     * Middle of [DevGameTests.EMPTY_5X3X5], one block above its floor.
     */
    private val CENTER = BlockPos(2, 1, 2)

    fun register(test: (String, Identifier, (GameTestHelper) -> Unit) -> Unit) {
        test("sided_item_capability", DevGameTests.EMPTY_1, ::sidedItemCapability)
        test("sided_config_syncs_to_client", DevGameTests.EMPTY_1, ::sidedConfigSyncsToClient)
        test("storage_fragments_save_load", DevGameTests.EMPTY_1, ::storageFragmentsSaveLoad)
        test("fluid_resource_handler_transactions", DevGameTests.EMPTY_1, ::fluidResourceHandlerTransactions)
        test("item_auto_io_pulls_and_pushes", DevGameTests.EMPTY_5X3X5, ::itemAutoIO)
        test("fluid_auto_io_pushes", DevGameTests.EMPTY_5X3X5, ::fluidAutoIO)
        test("multiblock_form_and_break", DevGameTests.EMPTY_5X3X5, ::multiblockFormAndBreak)
        test("multiblock_shared_state", DevGameTests.EMPTY_5X3X5, ::multiblockSharedState)
    }

    private fun machine(helper: GameTestHelper, pos: BlockPos = BlockPos.ZERO): DevMachineBlockEntity {
        helper.setBlock(pos, DevContent.DEV_MACHINE_BLOCK.get())
        return helper.getBlockEntity(pos, DevMachineBlockEntity::class.java)
    }

    private fun water(amount: Int): IFluidStack = IFluidStack.of(FluidStack(Fluids.WATER, amount))

    /**
     * Each face's item capability reaches the storage the sided configuration assigns to it; no side reaches all.
     */
    private fun sidedItemCapability(helper: GameTestHelper) {
        val be = machine(helper)
        val pos = helper.absolutePos(BlockPos.ZERO)
        be.itemConfig.update { it.cycleRelativeFacingStorageForward(Direction.EAST) }
        val diamond = ItemResource.of(Items.DIAMOND)

        val east = helper.level.getCapability(NeoCapabilities.Item.BLOCK, pos, Direction.EAST)!!
        val west = helper.level.getCapability(NeoCapabilities.Item.BLOCK, pos, Direction.WEST)!!
        val all = helper.level.getCapability(NeoCapabilities.Item.BLOCK, pos, null)!!
        helper.assertValueEqual(1, east.size(), "east handler size")
        helper.assertValueEqual(2, all.size(), "side-less handler size")
        Transaction.openRoot().use { tx ->
            east.insert(diamond, 3, tx)
            west.insert(diamond, 2, tx)
            tx.commit()
        }
        helper.assertValueEqual(3, be.inventory.get(1).stackSize(), "output slot (east face)")
        helper.assertValueEqual(2, be.inventory.get(0).stackSize(), "input slot (west face)")
        helper.assertTrue(be.getModule(Modules.ITEM_STORAGE, Direction.EAST) === be.output, "ITEM_STORAGE module follows the config")
        helper.succeed()
    }

    /**
     * Side configuration is DESCRIPTION scope, so screens on the client can show it.
     */
    private fun sidedConfigSyncsToClient(helper: GameTestHelper) {
        val be = machine(helper)
        be.itemConfig.update {
            it.cycleRelativeFacingStorageForward(Direction.UP)
            it.cycleRelativeFacingIOForward(Direction.UP)
        }
        be.fluidConfig.update { it.cycleRelativeFacingIOBackward(Direction.DOWN) }
        val registries = helper.level.registryAccess()
        val client = DevMachineBlockEntity(be.blockPos, be.blockState)
        client.handleUpdateTag(TagValueInput.create(ProblemReporter.DISCARDING, registries, be.getUpdateTag(registries)))
        val config = client.itemConfig.configuration
        helper.assertValueEqual(config.getStorageNameForRelativeFacing(Direction.UP), "output", "synced storage")
        helper.assertValueEqual(config.getIOForRelativeFacing(Direction.UP), EnumAutomaticIO.INPUT, "synced item IO")
        helper.assertValueEqual(client.fluidConfig.configuration.getIOForRelativeFacing(Direction.DOWN), EnumAutomaticIO.OUTPUT, "synced fluid IO")
        helper.assertTrue(client.inventory.get(1).isEmpty() && client.tanks.get(0).isEmpty(), "storages are not synced")
        helper.succeed()
    }

    private fun storageFragmentsSaveLoad(helper: GameTestHelper) {
        val be = machine(helper)
        val registries = helper.level.registryAccess()
        be.inventory.setSlot(1, IItemStack.of(ItemStack(Items.EMERALD, 7)))
        be.tanks.set(0, water(1234))
        be.fluidConfig.update { it.cycleRelativeFacingIOForward(Direction.NORTH) }

        val loaded = BlockEntity.loadStatic(be.blockPos, be.blockState, be.saveWithFullMetadata(registries), registries) as DevMachineBlockEntity
        helper.assertValueEqual(loaded.inventory.get(1).stackSize(), 7, "item count")
        helper.assertValueEqual(loaded.tanks.get(0).amount(), 1234, "fluid amount")
        helper.assertTrue(loaded.tanks.get(0).toMinecraft().`is`(Fluids.WATER), "fluid type")
        helper.assertValueEqual(loaded.fluidConfig.configuration.getIOForRelativeFacing(Direction.NORTH), EnumAutomaticIO.INPUT, "fluid IO")
        helper.succeed()
    }

    /**
     * The fluid ResourceHandler adapter: capacity, fluid matching, rollback on abort, one notification per commit.
     */
    private fun fluidResourceHandlerTransactions(helper: GameTestHelper) {
        var changes = 0
        val tanks = FluidStorageArray(2, 1000) { changes++ }
        val handler = WrapperResourceHandlerIFluidStorage.of(tanks)
        val waterRes = FluidResource.of(Fluids.WATER)
        val lavaRes = FluidResource.of(Fluids.LAVA)

        Transaction.openRoot().use { tx -> helper.assertValueEqual(handler.insert(0, waterRes, 1500, tx), 1000, "inserted up to capacity") }
        helper.assertTrue(tanks.get(0).isEmpty(), "aborted insert not rolled back")
        helper.assertValueEqual(changes, 0, "aborted transaction notified")

        Transaction.openRoot().use { tx ->
            handler.insert(0, waterRes, 600, tx)
            helper.assertValueEqual(handler.insert(0, lavaRes, 100, tx), 0, "other fluid into a filled tank")
            handler.insert(1, lavaRes, 100, tx)
            tx.commit()
        }
        helper.assertValueEqual(tanks.get(0).amount(), 600, "water")
        helper.assertValueEqual(tanks.get(1).amount(), 100, "lava")
        helper.assertValueEqual(changes, 1, "notifications on commit")

        Transaction.openRoot().use { tx ->
            helper.assertValueEqual(handler.extract(0, waterRes, 1000, tx), 600, "extracted")
            helper.assertValueEqual(handler.extract(1, waterRes, 10, tx), 0, "extracted the wrong fluid")
            tx.commit()
        }
        helper.assertTrue(tanks.get(0).isEmpty(), "drained tank is empty")
        helper.assertValueEqual(handler.getCapacityAsLong(0, waterRes), 1000L, "capacity")
        helper.succeed()
    }

    /**
     * An INPUT face pulls from the chest beside it into that face's storage; an OUTPUT face pushes its storage into
     * the chest beside it.
     */
    private fun itemAutoIO(helper: GameTestHelper) {
        val be = machine(helper, CENTER)
        helper.setBlock(CENTER.west(), Blocks.CHEST)
        helper.setBlock(CENTER.east(), Blocks.CHEST)
        val source = helper.getBlockEntity(CENTER.west(), ChestBlockEntity::class.java)
        val sink = helper.getBlockEntity(CENTER.east(), ChestBlockEntity::class.java)
        source.setItem(0, ItemStack(Items.DIAMOND, 10))
        be.inventory.setSlot(1, IItemStack.of(ItemStack(Items.GOLD_INGOT, 6)))
        be.itemConfig.update {
            it.cycleRelativeFacingIOForward(Direction.WEST) // INPUT
            it.cycleRelativeFacingStorageForward(Direction.EAST) // output slot
            it.cycleRelativeFacingIOBackward(Direction.EAST) // NONE -> OUTPUT
        }
        helper.succeedWhen {
            helper.assertValueEqual(be.inventory.get(0).stackSize(), 10, "diamonds pulled in")
            helper.assertTrue(source.isEmpty, "source chest emptied")
            helper.assertValueEqual(sink.countItem(Items.GOLD_INGOT), 6, "gold pushed out")
        }
    }

    private fun fluidAutoIO(helper: GameTestHelper) {
        val source = machine(helper, CENTER)
        val sink = machine(helper, CENTER.east())
        source.tanks.set(0, water(1000))
        source.fluidConfig.update { it.cycleRelativeFacingIOBackward(Direction.EAST) } // OUTPUT
        helper.runAfterDelay(1) {
            val moved = sink.tanks.get(0).amount()
            helper.assertTrue(moved <= DevMachineBlockEntity.MB_PER_TICK, "moved more than one operation's worth in a tick: $moved")
        }
        helper.succeedWhen {
            helper.assertValueEqual(sink.tanks.get(0).amount(), 1000, "water pushed into the neighbour")
            helper.assertTrue(source.tanks.get(0).isEmpty(), "source drained")
        }
    }

    private fun multiblock(helper: GameTestHelper, pos: BlockPos): DevMultiblockBlockEntity {
        helper.setBlock(pos, DevContent.DEV_MULTIBLOCK_BLOCK.get())
        return helper.getBlockEntity(pos, DevMultiblockBlockEntity::class.java)
    }

    /**
     * Two dev multiblock blocks side by side form a pair on their own once loaded (the west one is the `core`, at the
     * home slot); faces between the parts expose an empty storage; breaking one dissolves the structure, hands its
     * state to [DevCounter.onBreak], and frees the other.
     */
    private fun multiblockFormAndBreak(helper: GameTestHelper) {
        val core = multiblock(helper, CENTER)
        val wing = multiblock(helper, CENTER.east())
        val level = helper.level
        val at = helper.absolutePos(CENTER)
        // onLoad, which forms the structure, runs on the tick after placement.
        helper.runAfterDelay(2) {
            val coreMembership = core.part.membership
            helper.assertTrue(coreMembership != null && coreMembership.isHome, "core formed at the home slot: $coreMembership")
            helper.assertTrue(wing.part.membership?.offset == BlockPos(1, 0, 0), "wing offset: ${wing.part.membership}")
            helper.assertTrue(wing.part.membership?.structureId == coreMembership!!.structureId, "same structure")

            val inner = level.getCapability(NeoCapabilities.Item.BLOCK, at, Direction.EAST)
            val outer = level.getCapability(NeoCapabilities.Item.BLOCK, at, Direction.WEST)
            helper.assertValueEqual(inner?.size() ?: -1, 0, "face towards the other part")
            helper.assertValueEqual(outer?.size() ?: -1, 1, "outside face")
            helper.assertValueEqual(wing.itemConfig.configuration.getIOForAbsoluteFacing(Direction.WEST), EnumAutomaticIO.NONE, "wing's inner face IO")

            val state = core.counter()
            DevCounter.lastBroken = null
            helper.destroyBlock(CENTER.east())
            helper.assertTrue(DevCounter.lastBroken === state && state?.breaks == 1, "the shared state is told once")
            helper.assertTrue(core.part.membership == null, "core still formed")
            helper.assertValueEqual(level.getCapability(NeoCapabilities.Item.BLOCK, at, Direction.EAST)?.size() ?: -1, 1, "inner face after break")
            helper.succeed()
        }
    }

    /**
     * The structure ticks once per game tick however many members tick; every member sees the home member's state;
     * only the home member saves it.
     */
    private fun multiblockSharedState(helper: GameTestHelper) {
        val core = multiblock(helper, CENTER)
        val wing = multiblock(helper, CENTER.east())
        var first = 0
        helper.runAfterDelay(3) { first = core.counter()?.count ?: -1 }
        helper.runAfterDelay(13) {
            val count = core.counter()?.count ?: -1
            helper.assertValueEqual(count - first, 10, "ticks counted over 10 game ticks")
            helper.assertTrue(wing.counter() === core.part.state, "wing does not see the home member's state")
            helper.assertTrue(wing.part.state == null, "wing holds a state of its own")

            val registries = helper.level.registryAccess()
            val loaded = BlockEntity.loadStatic(core.blockPos, core.blockState, core.saveWithFullMetadata(registries), registries)
                as DevMultiblockBlockEntity
            helper.assertValueEqual((loaded.part.state as? DevCounter)?.count ?: -1, count, "saved state")
            val loadedWing = BlockEntity.loadStatic(wing.blockPos, wing.blockState, wing.saveWithFullMetadata(registries), registries)
                as DevMultiblockBlockEntity
            helper.assertTrue(loadedWing.part.state == null && loadedWing.part.membership == wing.part.membership, "wing saves only its membership")
            helper.succeed()
        }
    }
}
