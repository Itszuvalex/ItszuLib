package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.Capabilities
import com.itszuvalex.itszulib.api.Components
import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.storage.ItemStorageArray
import com.itszuvalex.itszulib.api.storage.ItemStorageResourceHandler
import com.itszuvalex.itszulib.api.utility.Loc4
import com.itszuvalex.itszulib.api.wrappers.WrapperBlockEntity
import com.itszuvalex.itszulib.api.wrappers.WrapperResourceHandlerIItemStorage
import com.itszuvalex.itszulib.util.Color
import com.itszuvalex.itszulib.verify.BlockEntityRoundTrip
import com.itszuvalex.itszulib.verify.BreakChecks
import com.itszuvalex.itszulib.verify.CapabilityChecks
import com.itszuvalex.itszulib.verify.MenuChecks
import com.itszuvalex.itszulib.verify.TickChecks
import com.itszuvalex.itszulib.verify.ContentIntegrity
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.FunctionGameTestInstance
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.TestData
import net.minecraft.resources.Identifier
import net.minecraft.util.ProblemReporter
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.storage.TagValueInput
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModList
import net.neoforged.neoforge.event.RegisterGameTestsEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.transfer.item.ItemResource
import net.neoforged.neoforge.transfer.transaction.Transaction
import java.util.function.Consumer
import net.neoforged.neoforge.capabilities.Capabilities as NeoCapabilities

/**
 * Game tests for the block entity framework, run with `./gradlew runGameTestServer`. Each uses vanilla's 1x1x1
 * `minecraft:empty` structure.
 */
object DevGameTests {
    private val POS: BlockPos = BlockPos.ZERO

    private val TEST_FUNCTIONS: DeferredRegister<Consumer<GameTestHelper>> =
        DeferredRegister.create(Registries.TEST_FUNCTION, ItszuLib.ID)

    private val TESTS = mutableListOf<Pair<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>, Identifier>>()

    /**
     * Vanilla's 1x1x1 structure.
     */
    @JvmField
    val EMPTY_1: Identifier = Identifier.withDefaultNamespace("empty")

    /**
     * An empty 5x3x5 structure (`data/itszulib/structure/dev_5x3x5.nbt`) for tests that need neighbours.
     */
    @JvmField
    val EMPTY_5X3X5: Identifier = Identifier.fromNamespaceAndPath(ItszuLib.ID, "dev_5x3x5")

    /**
     * Registers a game test named [name] that runs [body] in [structure].
     */
    fun test(name: String, structure: Identifier, body: (GameTestHelper) -> Unit) {
        TESTS += TEST_FUNCTIONS.register(name) { -> Consumer<GameTestHelper> { body(it) } } to structure
    }

    /**
     * Registers a game test named [name] that runs [body] in [EMPTY_1].
     */
    fun test(name: String, body: (GameTestHelper) -> Unit) = test(name, EMPTY_1, body)

    init {
        test("mod_loaded") { helper ->
            helper.assertTrue(ModList.get().isLoaded(ItszuLib.ID), "ItszuLib is not in the mod list")
            helper.succeed()
        }
        test("content_integrity", ::contentIntegrity)
        test("block_entities_save_load_and_sync", ::blockEntityRoundTrips)
        test("block_entities_break_as_they_declare", ::breakChecks)
        test("block_entities_run_without_failing", ::tickChecks)
        test("capabilities_match_modules", ::capabilityChecks)
        test("menus_neither_lose_nor_make_items", ::menuChecks)
        test("colorable_capability", ::colorableCapability)
        test("level_save_load", ::levelSaveLoad)
        test("client_update_tag", ::clientUpdateTag)
        test("item_capability_insert", ::itemCapabilityInsert)
        test("drops_inventory_on_break", ::dropsInventoryOnBreak)
        test("level_lookup_returns_core", ::levelLookupReturnsCore)
        test("inventory_change_marks_dirty", ::inventoryChangeMarksDirty)
        test("item_scope_components_round_trip", ::itemScopeComponentsRoundTrip)
        test("resource_handler_per_slot_limit", ::resourceHandlerPerSlotLimit)
        test("handler_adapter_joins_open_transaction", ::handlerAdapterJoinsOpenTransaction)
        DevStorageGameTests.register(::test)
        DevMenuGameTests.register(::test)
        DevResearchGameTests.register(::test)
        DevBreakGameTests.register(::test)
    }

    fun register(modBus: IEventBus) {
        TEST_FUNCTIONS.register(modBus)
        modBus.addListener(::registerTests)
    }

    private fun registerTests(event: RegisterGameTestsEvent) {
        val environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(ItszuLib.ID, "default"))
        TESTS.forEach { (test, structure) ->
            event.registerTest(
                test.id,
                FunctionGameTestInstance(test.key, TestData(environment, structure, 100, 0, true)),
            )
        }
    }

    private fun place(helper: GameTestHelper): DevFragBlockEntity {
        helper.setBlock(POS, DevContent.DEV_FRAG_BLOCK.get())
        return helper.getBlockEntity(POS, DevFragBlockEntity::class.java)
    }

    private fun menuChecks(helper: GameTestHelper) {
        helper.assertValueEqual(MenuChecks.problems(helper.level, helper.absolutePos(POS), ItszuLib.ID, helper.makeMockServerPlayerInLevel()), emptyList<String>(), "menu problems")
        helper.succeed()
    }

    private fun capabilityChecks(helper: GameTestHelper) {
        helper.assertValueEqual(CapabilityChecks.problems(helper.level, helper.absolutePos(POS), ItszuLib.ID), emptyList<String>(), "capability problems")
        helper.succeed()
    }

    private fun tickChecks(helper: GameTestHelper) {
        helper.assertValueEqual(TickChecks.problems(helper.level, helper.absolutePos(POS), ItszuLib.ID), emptyList<String>(), "tick problems")
        helper.succeed()
    }

    private fun breakChecks(helper: GameTestHelper) {
        helper.assertValueEqual(BreakChecks.problems(helper.level, helper.absolutePos(POS), ItszuLib.ID), emptyList<String>(), "break problems")
        helper.succeed()
    }

    private fun blockEntityRoundTrips(helper: GameTestHelper) {
        helper.assertValueEqual(BlockEntityRoundTrip.problems(helper.level, helper.absolutePos(POS), ItszuLib.ID), emptyList<String>(), "round trip problems")
        helper.succeed()
    }

    private fun contentIntegrity(helper: GameTestHelper) {
        helper.assertValueEqual(ContentIntegrity.blockEntityProblems(ItszuLib.ID), emptyList<String>(), "block entity problems")
        helper.succeed()
    }

    private fun colorableCapability(helper: GameTestHelper) {
        val be = place(helper)
        val level = helper.level
        val pos = helper.absolutePos(POS)

        val colorable = level.getCapability(Capabilities.COLORABLE, pos, null)
        helper.assertTrue(colorable != null, "COLORABLE capability missing")
        val chunk = level.getChunkAt(pos)
        chunk.tryMarkSaved()
        val color = colorable!!.getColor().withRed(42)
        colorable.setColor(color)
        helper.assertTrue(chunk.isUnsaved, "setColor through the capability did not mark dirty")
        helper.assertValueEqual(color, be.getModule(Modules.COLORABLE, null)!!.getColor(), "BlockEntityCore#getModule")
        helper.assertValueEqual(color, WrapperBlockEntity(be).getModule(Modules.COLORABLE, null)!!.getColor(), "WrapperBlockEntity#getModule")
        helper.assertTrue(level.getCapability(NeoCapabilities.Energy.BLOCK, pos, null) == null, "Unexposed capability should be null")
        helper.succeed()
    }

    private fun levelSaveLoad(helper: GameTestHelper) {
        val be = place(helper)
        val registries = helper.level.registryAccess()
        be.colorable.setColor(be.colorable.getColor().withGreen(7))
        be.inventory.setSlot(0, IItemStack.of(ItemStack(Items.DIAMOND, 3)))

        val tag = be.saveWithFullMetadata(registries)
        val loaded = BlockEntity.loadStatic(be.blockPos, be.blockState, tag, registries)

        helper.assertTrue(loaded is DevFragBlockEntity, "Loaded wrong block entity: $loaded")
        val copy = loaded as DevFragBlockEntity
        helper.assertValueEqual(be.colorable.getColor(), copy.colorable.getColor(), "color")
        helper.assertValueEqual(3, copy.inventory.get(0).stackSize(), "inventory count")
        helper.assertTrue(copy.inventory.get(0).toMinecraft().`is`(Items.DIAMOND), "inventory item")
        helper.succeed()
    }

    private fun clientUpdateTag(helper: GameTestHelper) {
        val be = place(helper)
        val registries = helper.level.registryAccess()
        be.colorable.setColor(be.colorable.getColor().withBlue(99))
        be.inventory.setSlot(0, IItemStack.of(ItemStack(Items.DIAMOND)))

        val tag = be.getUpdateTag(registries)
        val client = DevFragBlockEntity(be.blockPos, be.blockState)
        client.handleUpdateTag(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag))

        helper.assertValueEqual(99.toByte(), client.getModule(Modules.COLORABLE, null)!!.getColor().blue, "synced color")
        helper.assertTrue(client.inventory.get(0).isEmpty(), "Inventory is LEVEL scope only and must not sync")
        helper.succeed()
    }

    private fun itemCapabilityInsert(helper: GameTestHelper) {
        val be = place(helper)
        val handler = helper.level.getCapability(NeoCapabilities.Item.BLOCK, helper.absolutePos(POS), null)
        helper.assertTrue(handler != null, "Item BLOCK capability missing")

        Transaction.openRoot().use { tx ->
            handler!!.insert(ItemResource.of(Items.DIAMOND), 5, tx)
            // Aborted: must roll back
        }
        helper.assertTrue(be.inventory.get(0).isEmpty(), "Aborted transaction was not rolled back")

        Transaction.openRoot().use { tx ->
            helper.assertValueEqual(5, handler!!.insert(ItemResource.of(Items.DIAMOND), 5, tx), "inserted")
            tx.commit()
        }
        helper.assertValueEqual(5, be.inventory.get(0).stackSize(), "committed count")
        helper.succeed()
    }

    private fun dropsInventoryOnBreak(helper: GameTestHelper) {
        val be = place(helper)
        be.inventory.setSlot(0, IItemStack.of(ItemStack(Items.DIAMOND, 2)))
        helper.destroyBlock(POS)
        helper.succeedWhen { helper.assertItemEntityPresent(Items.DIAMOND, POS, 2.0) }
    }

    /**
     * Location lookups must hand back the BlockEntityCore itself, not a capability-only wrapper, so modules without a
     * block capability (e.g. network modules) stay reachable.
     */
    private fun levelLookupReturnsCore(helper: GameTestHelper) {
        val be = place(helper)
        val level = helper.level
        val pos = helper.absolutePos(POS)
        helper.assertTrue(ILevel.of(level).getIBlockEntity(pos) === be, "ILevel#getIBlockEntity wrapped the core")
        helper.assertTrue(Loc4.of(level, pos).getIBlockEntity(false) === be, "Loc4Level#getIBlockEntity wrapped the core")
        helper.assertTrue(Loc4.of(ILevel.of(level), pos).getIBlockEntity(false) === be, "Loc4ILevel#getIBlockEntity wrapped the core")
        helper.succeed()
    }

    /**
     * Inventory changes must mark the block entity (and so its chunk) for saving: directly, and through the item
     * capability on commit only.
     */
    private fun inventoryChangeMarksDirty(helper: GameTestHelper) {
        val be = place(helper)
        val pos = helper.absolutePos(POS)
        val chunk = helper.level.getChunkAt(pos)
        val handler = helper.level.getCapability(NeoCapabilities.Item.BLOCK, pos, null)
        helper.assertTrue(handler != null, "Item BLOCK capability missing")

        chunk.tryMarkSaved()
        be.inventory.setSlot(0, IItemStack.of(ItemStack(Items.DIAMOND)))
        helper.assertTrue(chunk.isUnsaved, "Direct inventory change did not mark dirty")

        chunk.tryMarkSaved()
        Transaction.openRoot().use { tx -> handler!!.insert(0, ItemResource.of(Items.DIAMOND), 1, tx) }
        helper.assertFalse(chunk.isUnsaved, "Aborted transaction marked dirty")

        Transaction.openRoot().use { tx ->
            handler!!.insert(0, ItemResource.of(Items.DIAMOND), 1, tx)
            tx.commit()
        }
        helper.assertTrue(chunk.isUnsaved, "Committed transaction did not mark dirty")
        helper.succeed()
    }

    /**
     * ITEM-scope fragments travel through the block's item components (as loot copy_components and pick-block do);
     * LEVEL-only state such as the inventory does not.
     */
    private fun itemScopeComponentsRoundTrip(helper: GameTestHelper) {
        val be = place(helper)
        val color = Color(0xFF123456.toInt())
        be.colorable.setColor(color)
        be.inventory.setSlot(0, IItemStack.of(ItemStack(Items.DIAMOND)))

        val components = be.collectComponents()
        helper.assertTrue(components.has(Components.FRAGMENT_DATA.get()), "fragment_data component missing")

        val placed = DevFragBlockEntity(be.blockPos, be.blockState)
        placed.applyComponents(components, DataComponentPatch.EMPTY)

        helper.assertValueEqual(color, placed.colorable.getColor(), "color from item")
        helper.assertTrue(placed.inventory.get(0).isEmpty(), "Inventory is LEVEL scope only and must not ride on the item")
        helper.succeed()
    }

    /**
     * Regression (REVIEW O4): a chest's handler adapted to an ItszuLib storage and exposed again through ItszuLib's
     * NeoForge wrapper. Inserting through the wrapper runs inside the caller's transaction; the adapter used to open a
     * root transaction there, which threw. It now joins it: aborting rolls the chest back, committing keeps the items.
     */
    private fun handlerAdapterJoinsOpenTransaction(helper: GameTestHelper) {
        helper.setBlock(BlockPos.ZERO, Blocks.CHEST)
        val chest = helper.level.getCapability(NeoCapabilities.Item.BLOCK, helper.absolutePos(BlockPos.ZERO), null)!!
        val exposed = WrapperResourceHandlerIItemStorage.of(ItemStorageResourceHandler(chest))
        val diamond = ItemResource.of(Items.DIAMOND)

        Transaction.openRoot().use { tx -> helper.assertValueEqual(exposed.insert(0, diamond, 5, tx), 5, "inserted inside the transaction") }
        helper.assertValueEqual(chest.getAmountAsInt(0), 0, "aborted insert left in the chest")

        Transaction.openRoot().use { tx ->
            exposed.insert(0, diamond, 5, tx)
            tx.commit()
        }
        helper.assertValueEqual(chest.getAmountAsInt(0), 5, "committed insert")
        helper.succeed()
    }

    /**
     * The item ResourceHandler adapter must respect IItemStorage's per-slot limit and notify only on commit.
     */
    private fun resourceHandlerPerSlotLimit(helper: GameTestHelper) {
        var changes = 0
        val storage = object : ItemStorageArray(2, Runnable { changes++ }) {
            override fun maxStackSize(index: Int): Int = if (index == 0) 4 else super.maxStackSize(index)
        }
        val handler = WrapperResourceHandlerIItemStorage.of(storage)
        val diamond = ItemResource.of(Items.DIAMOND)

        helper.assertValueEqual(4L, handler.getCapacityAsLong(0, diamond), "slot 0 capacity")
        helper.assertValueEqual(64L, handler.getCapacityAsLong(1, diamond), "slot 1 capacity")

        Transaction.openRoot().use { tx -> helper.assertValueEqual(4, handler.insert(0, diamond, 10, tx), "inserted into limited slot") }
        helper.assertValueEqual(0, changes, "aborted transaction notified")
        helper.assertTrue(storage.get(0).isEmpty(), "aborted transaction not rolled back")

        Transaction.openRoot().use { tx ->
            handler.insert(0, diamond, 10, tx)
            handler.insert(1, diamond, 10, tx)
            tx.commit()
        }
        helper.assertValueEqual(4, storage.get(0).stackSize(), "limited slot count")
        helper.assertValueEqual(10, storage.get(1).stackSize(), "unlimited slot count")
        helper.assertValueEqual(1, changes, "notifications on commit")
        helper.succeed()
    }
}
