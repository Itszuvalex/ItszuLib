package com.itszuvalex.itszulib.verify

import com.itszuvalex.itszulib.api.Components
import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.core.BlockEntityCore
import com.itszuvalex.itszulib.core.BreakBehavior
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.phys.AABB
import java.util.TreeMap

/**
 * Breaks every block entity a mod registers, full, and checks that what happens to the contents is what each fragment
 * says ([BreakBehavior]), for the mod's own game tests:
 * - what a [BreakBehavior.DROP] fragment holds appears as item entities, exactly;
 * - what a [BreakBehavior.KEEP] fragment holds is on the one dropped block item (`itszulib:fragment_data`), and placing
 *   that item back (applying its components to a new block entity) restores it exactly; with nothing to keep the
 *   dropped item is plain, so empty blocks stack;
 * - no item in a storage the block entity exposes as [Modules.ITEM_STORAGE] is lost without a fragment declaring
 *   [BreakBehavior.DISCARD] for it, which finds block entities that forgot to say what happens to their items.
 *
 * Contents are given with [BlockEntityContents.fill]; contents of kinds without a [BlockEntityContents.Probe] are not
 * checked. The block is broken with `destroyBlock(pos, true)` (the survival path: loot table, explosions and
 * `Block.dropResources` all go through it), item entities near it are removed afterwards.
 */
object BreakChecks {
    private val DROPS = setOf(BreakBehavior.DROP)
    private val KEEPS = setOf(BreakBehavior.KEEP)
    private val DISCARDS = setOf(BreakBehavior.DISCARD)

    /**
     * @param pos A position in [level] with room for a block and the items it drops, which is left empty.
     */
    @JvmStatic
    fun problems(level: ServerLevel, pos: BlockPos, namespace: String): List<String> {
        val problems = ArrayList<String>()
        val blocks = BuiltInRegistries.BLOCK.entrySet()
            .filter { it.key.identifier().namespace == namespace && it.value is EntityBlock }
            .sortedBy { it.key.identifier().path }
        for ((key, block) in blocks) {
            val name = key.identifier().toString()
            try {
                level.setBlock(pos, block.defaultBlockState(), 3)
                val be = level.getBlockEntity(pos) as? BlockEntityCore ?: continue
                if (!BlockEntityContents.fill(be) && moduleItems(be).isEmpty()) continue
                val expectDropped = items(BlockEntityContents.tally(be, DROPS))
                val expectKept = BlockEntityContents.tally(be, KEEPS)
                val excused = items(BlockEntityContents.tally(be, DISCARDS))
                val inModules = moduleItems(be)

                level.destroyBlock(pos, true)
                val entities = level.getEntitiesOfClass(ItemEntity::class.java, AABB(pos).inflate(4.0))
                val (blockItems, others) = entities.map { it.item }.partition { it.`is`(block.asItem()) }
                val dropped = TreeMap<String, Long>()
                for (stack in others) dropped.merge(BlockEntityContents.itemKey(stack), stack.count.toLong(), Long::plus)
                val droppedItems = BlockEntityContents.Tally(dropped)

                val missing = expectDropped.missingFrom(droppedItems)
                val extra = droppedItems.missingFrom(expectDropped)
                if (missing.isNotEmpty()) problems += "$name should drop $missing but dropped $droppedItems"
                if (extra.isNotEmpty()) problems += "$name dropped $extra it was not holding as DROP contents"

                val carried = blockItems.filter { it.has(Components.FRAGMENT_DATA.get()) }
                var restoredInModules = BlockEntityContents.Tally.EMPTY
                if (!expectKept.isEmpty()) {
                    if (carried.size != 1) {
                        problems += "$name holds $expectKept to keep but dropped ${carried.size} items carrying data (of ${blockItems.size} block items)"
                    } else {
                        level.setBlock(pos, block.defaultBlockState(), 3)
                        val placed = level.getBlockEntity(pos) as BlockEntityCore
                        placed.applyComponentsFromItemStack(carried[0])
                        val restored = BlockEntityContents.tally(placed, KEEPS)
                        if (restored != expectKept) problems += "$name kept $expectKept but placing its item gives $restored"
                        restoredInModules = moduleItems(placed)
                    }
                } else if (carried.isNotEmpty()) {
                    problems += "$name had nothing to keep but dropped an item carrying data"
                }

                val lost = items(inModules).missingFrom(
                    droppedItems + restoredInModules + excused,
                )
                if (lost.isNotEmpty()) problems += "$name lost $lost from its item storage: no fragment drops, keeps or discards it"
            } catch (e: Exception) {
                problems += "$name threw ${e::class.java.simpleName}: ${e.message}"
            } finally {
                level.getEntitiesOfClass(ItemEntity::class.java, AABB(pos).inflate(4.0)).forEach { it.discard() }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)
            }
        }
        return problems
    }

    private fun items(tally: BlockEntityContents.Tally) = BlockEntityContents.Tally(tally.counts.filterKeys { '#' in it })

    /** What the item storages [be] exposes as a module (with no side) hold. */
    private fun moduleItems(be: BlockEntityCore): BlockEntityContents.Tally {
        val counts = TreeMap<String, Long>()
        val storage = be.getModule(Modules.ITEM_STORAGE, null) ?: return BlockEntityContents.Tally.EMPTY
        for (i in 0 until storage.size()) {
            val stack: ItemStack = storage.get(i).toMinecraft()
            if (!stack.isEmpty) counts.merge(BlockEntityContents.itemKey(stack), stack.count.toLong(), Long::plus)
        }
        return BlockEntityContents.Tally(counts)
    }
}
