package com.itszuvalex.itszulib.verify

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.Module
import com.itszuvalex.itszulib.core.BlockEntityCore
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.neoforged.neoforge.capabilities.BlockCapability
import net.neoforged.neoforge.capabilities.Capabilities as NeoCapabilities

/**
 * Checks that what each block entity exposes to its own code (modules) matches what other mods can reach through
 * NeoForge's capability system, for the mod's own game tests. For every [BlockEntityCore] block, on every face and with
 * no face, and every module that has a block capability:
 * - a module the block entity provides must be reachable as its capability ([level] `getCapability`), and the same
 *   object; if it is not, the block entity type was probably never passed to
 *   [com.itszuvalex.itszulib.api.ModuleCapabilities.registerBlockEntity];
 * - a capability the world offers for a module's capability on a face where the block entity provides no module
 *   would be a stale or mis-sided provider.
 *
 * NeoForge's item, fluid and energy handlers wrap the storage module in a handler, so only their presence is checked:
 * a face with a storage module must offer the handler. (A handler without a module is allowed: a block entity may
 * offer one over storage it does not expose as a module.)
 */
object CapabilityChecks {
    private val HANDLERS: List<Pair<com.itszuvalex.itszulib.api.adapters.IModule<*>, BlockCapability<*, Direction?>>> = listOf(
        Modules.ITEM_STORAGE to NeoCapabilities.Item.BLOCK,
        Modules.FLUID_STORAGE to NeoCapabilities.Fluid.BLOCK,
        Modules.ENERGY_STORAGE to NeoCapabilities.Energy.BLOCK,
    )

    /**
     * @param pos A position in [level] with room for a block, which is left empty.
     */
    @JvmStatic
    fun problems(level: ServerLevel, pos: BlockPos, namespace: String): List<String> {
        val problems = ArrayList<String>()
        val blocks = BuiltInRegistries.BLOCK.entrySet()
            .filter { it.key.identifier().namespace == namespace && it.value is EntityBlock }
            .sortedBy { it.key.identifier().path }
        val sides = listOf<Direction?>(null) + Direction.entries
        for ((key, block) in blocks) {
            val name = key.identifier().toString()
            try {
                level.setBlock(pos, block.defaultBlockState(), 3)
                val be = level.getBlockEntity(pos) as? BlockEntityCore ?: continue
                for (module in Module.modules()) {
                    val cap = module.blockCapability ?: continue
                    @Suppress("UNCHECKED_CAST")
                    cap as BlockCapability<Any, Direction?>
                    for (side in sides) {
                        val internal = be.getModule(module, side)
                        val external = level.getCapability(cap, pos, side)
                        val where = "${module.id} on ${side ?: "no face"}"
                        if (internal != null && external == null) {
                            problems += "$name provides $where but it is not reachable as ${cap.name()} (is its block entity type registered with ModuleCapabilities?)"
                        } else if (internal != null && external !== internal) {
                            problems += "$name provides $where but ${cap.name()} gives a different object"
                        } else if (internal == null && external != null) {
                            problems += "$name offers ${cap.name()} on ${side ?: "no face"} but provides no ${module.id} there"
                        }
                    }
                }
                for ((module, cap) in HANDLERS) {
                    for (side in sides) {
                        val has = be.getModule(module, side) != null
                        val reachable = level.getCapability(cap, pos, side) != null
                        if (has && !reachable) problems += "$name provides ${module.id} on ${side ?: "no face"} but offers no ${cap.name()} there"
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
