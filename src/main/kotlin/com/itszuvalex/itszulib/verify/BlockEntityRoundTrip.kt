package com.itszuvalex.itszulib.verify

import com.itszuvalex.itszulib.core.BlockEntityCore
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.ProblemReporter
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.storage.TagValueInput

/**
 * Save, load and client sync checks over every block entity a mod registers, for the mod's own game tests: a block
 * entity that cannot be written and read back, or whose client copy differs from what the server sent, is found the
 * first time anyone runs the test instead of by a player losing a machine's contents.
 *
 * For each block of the namespace that is an [EntityBlock], [problems] places it with its default state, then checks
 * that:
 * - saving ([BlockEntity.saveWithFullMetadata]) and loading it ([BlockEntity.loadStatic]) gives the same type of
 *   block entity, and saving that again gives the same data;
 * - a fresh block entity that applies the server's update tag ([BlockEntity.getUpdateTag], [BlockEntity.handleUpdateTag])
 *   reports the same update tag back, so the client sees everything the server sent.
 *
 * Nothing throws out of it: an exception is a problem too. The block is removed afterwards.
 */
object BlockEntityRoundTrip {
    /**
     * @param pos A position in [level] with room for a block, which is overwritten and left empty.
     * @param prepare Called with each block entity after it is placed, to give it contents worth saving; by default
     *   [BlockEntityContents.fill]. The loaded copy must then hold the same ([BlockEntityContents.tally]).
     */
    @JvmStatic
    @JvmOverloads
    fun problems(level: ServerLevel, pos: BlockPos, namespace: String, prepare: (BlockEntity) -> Unit = { if (it is BlockEntityCore) BlockEntityContents.fill(it) }): List<String> {
        val problems = ArrayList<String>()
        val registries = level.registryAccess()
        val blocks = BuiltInRegistries.BLOCK.entrySet()
            .filter { it.key.identifier().namespace == namespace && it.value is EntityBlock }
            .sortedBy { it.key.identifier().path }
        for ((key, block) in blocks) {
            val name = key.identifier().toString()
            try {
                level.setBlock(pos, block.defaultBlockState(), 3)
                val be = level.getBlockEntity(pos)
                if (be == null) {
                    problems += "$name placed no block entity"
                    continue
                }
                prepare(be)
                val before = (be as? BlockEntityCore)?.let { BlockEntityContents.tally(it) }

                val saved = be.saveWithFullMetadata(registries)
                val loaded = BlockEntity.loadStatic(pos, be.blockState, saved, registries)
                if (loaded == null || loaded.type != be.type) {
                    problems += "$name saved as $saved loads as $loaded"
                } else if (before != null && BlockEntityContents.tally(loaded as BlockEntityCore) != before) {
                    problems += "$name holds $before but its loaded copy holds ${BlockEntityContents.tally(loaded)}"
                } else if (loaded.saveWithFullMetadata(registries) != saved) {
                    problems += "$name changes when saved, loaded and saved again: $saved, then ${loaded.saveWithFullMetadata(registries)}"
                }

                val update = be.getUpdateTag(registries)
                val client = be.type.create(pos, be.blockState)
                if (client == null) {
                    problems += "$name's block entity type creates nothing for a client copy"
                } else {
                    client.handleUpdateTag(TagValueInput.create(ProblemReporter.DISCARDING, registries, update))
                    if (client.getUpdateTag(registries) != update) {
                        problems += "$name's client copy reports ${client.getUpdateTag(registries)} after being sent $update"
                    }
                }
            } catch (e: Exception) {
                problems += "$name threw ${e::class.java.simpleName}: ${e.message}"
            } finally {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)
            }
        }
        return problems
    }
}
