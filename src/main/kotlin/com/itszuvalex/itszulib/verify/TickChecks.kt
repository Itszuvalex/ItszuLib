package com.itszuvalex.itszulib.verify

import com.itszuvalex.itszulib.core.BlockEntityCore
import com.itszuvalex.itszulib.core.frag.FragEnergyStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.phys.AABB

/**
 * Runs every block entity a mod registers for a while, for the mod's own game tests: alone, and with a chest holding
 * items on every side (so item and fluid IO has something to move and nothing to refuse it), and checks it
 * - does not throw from its ticker, saving or loading,
 * - stays the block entity of its block, and still saves and loads what it holds afterwards,
 * - keeps every battery it owns within `[0, max]` and finite.
 *
 * Contents are given with [BlockEntityContents.fill]. Tickers are called directly, [ticks] times, on the server side;
 * this does not replace watching a machine work (that needs a game test of its own), but it finds machines that cannot
 * run at all.
 */
object TickChecks {
    /**
     * @param pos A position in [level] with a free block on every side, which is left empty.
     */
    @JvmStatic
    @JvmOverloads
    fun problems(level: ServerLevel, pos: BlockPos, namespace: String, ticks: Int = 200): List<String> {
        val problems = ArrayList<String>()
        val blocks = BuiltInRegistries.BLOCK.entrySet()
            .filter { it.key.identifier().namespace == namespace && it.value is EntityBlock }
            .sortedBy { it.key.identifier().path }
        for ((key, block) in blocks) {
            for (neighbours in listOf(false, true)) {
                val name = "${key.identifier()}${if (neighbours) " beside chests" else " alone"}"
                try {
                    if (neighbours) for (d in Direction.entries) placeChest(level, pos.relative(d))
                    level.setBlock(pos, block.defaultBlockState(), 3)
                    val be = level.getBlockEntity(pos) as? BlockEntityCore ?: continue
                    BlockEntityContents.fill(be)
                    repeat(ticks) { tick ->
                        val state = level.getBlockState(pos)
                        val current = level.getBlockEntity(pos)
                        if (current !== be) {
                            problems += "$name stopped being the block entity at its position after $tick ticks"
                            return@repeat
                        }
                        @Suppress("UNCHECKED_CAST")
                        (state.getTicker(level, be.type as net.minecraft.world.level.block.entity.BlockEntityType<BlockEntity>)
                            as? net.minecraft.world.level.block.entity.BlockEntityTicker<BlockEntity>)?.tick(level, pos, state, be)
                    }
                    for (frag in be.contentFragments().filterIsInstance<FragEnergyStorage>()) {
                        val stored = frag.storage.storage()
                        if (stored.isNaN() || stored < 0.0 || stored > frag.storage.maxStorage()) {
                            problems += "$name ended with $stored energy in a battery of ${frag.storage.maxStorage()}"
                        }
                    }
                    val saved = be.saveWithFullMetadata(level.registryAccess())
                    val loaded = BlockEntity.loadStatic(pos, be.blockState, saved, level.registryAccess())
                    if (loaded == null || loaded.saveWithFullMetadata(level.registryAccess()) != saved) {
                        problems += "$name does not save and load what it holds after running"
                    }
                } catch (e: Exception) {
                    problems += "$name threw ${e::class.java.simpleName}: ${e.message}"
                } finally {
                    level.getEntitiesOfClass(ItemEntity::class.java, AABB(pos).inflate(4.0)).forEach { it.discard() }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)
                    for (d in Direction.entries) level.setBlock(pos.relative(d), Blocks.AIR.defaultBlockState(), 3)
                }
            }
        }
        return problems
    }

    private fun placeChest(level: ServerLevel, at: BlockPos) {
        level.setBlock(at, Blocks.CHEST.defaultBlockState(), 3)
        val chest = level.getBlockEntity(at) as? ChestBlockEntity ?: return
        for (i in 0 until 9) chest.setItem(i, ItemStack(Items.COBBLESTONE, 16))
    }
}
