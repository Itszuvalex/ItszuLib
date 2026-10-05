package com.itszuvalex.itszulib.verify

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.core.BlockEntityCore
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.phys.AABB
import java.util.TreeMap

/**
 * Opens the menu of every block entity a mod registers, full, and clicks every slot every way a player can, checking
 * that no click makes items appear or vanish, for the mod's own game tests. Items are counted in every slot of the menu
 * (the block's and the player's), the carried stack and item entities near the block, so a click that loses a stack
 * into a storage that hands out copies, or duplicates one on a partial move, or drops it without an entity, fails.
 *
 * For each slot: left and right click, shift-click, swap with hotbar slot 0 and 1, throw, and double-click (collect
 * all), in that order, without putting anything back in between, so each starts from whatever the last left. Then the
 * menu closes. The player starts with a few stacks, some the same as the block's contents so they merge.
 */
object MenuChecks {
    private val CLICKS = listOf(
        "left click" to { menu: AbstractContainerMenu, slot: Int, player: ServerPlayer -> menu.clicked(slot, 0, ContainerInput.PICKUP, player) },
        "right click" to { menu: AbstractContainerMenu, slot: Int, player: ServerPlayer -> menu.clicked(slot, 1, ContainerInput.PICKUP, player) },
        "shift-click" to { menu: AbstractContainerMenu, slot: Int, player: ServerPlayer -> menu.clicked(slot, 0, ContainerInput.QUICK_MOVE, player) },
        "swap with hotbar 0" to { menu: AbstractContainerMenu, slot: Int, player: ServerPlayer -> menu.clicked(slot, 0, ContainerInput.SWAP, player) },
        "swap with hotbar 1" to { menu: AbstractContainerMenu, slot: Int, player: ServerPlayer -> menu.clicked(slot, 1, ContainerInput.SWAP, player) },
        "throw" to { menu: AbstractContainerMenu, slot: Int, player: ServerPlayer -> menu.clicked(slot, 0, ContainerInput.THROW, player) },
        "double-click" to { menu: AbstractContainerMenu, slot: Int, player: ServerPlayer -> menu.clicked(slot, 0, ContainerInput.PICKUP_ALL, player) },
    )

    /**
     * @param pos A position in [level] with room for a block and the items thrown from it, which is left empty.
     * @param player A player in [level]; its game mode is set to survival, it is moved to [pos] (where thrown items land) and its inventory is replaced.
     */
    @JvmStatic
    fun problems(level: ServerLevel, pos: BlockPos, namespace: String, player: ServerPlayer): List<String> {
        val problems = ArrayList<String>()
        val blocks = BuiltInRegistries.BLOCK.entrySet()
            .filter { it.key.identifier().namespace == namespace && it.value is EntityBlock }
            .sortedBy { it.key.identifier().path }
        player.setGameMode(GameType.SURVIVAL)
        player.snapTo(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        for ((key, block) in blocks) {
            val name = key.identifier().toString()
            var menu: AbstractContainerMenu? = null
            try {
                level.setBlock(pos, block.defaultBlockState(), 3)
                val be = level.getBlockEntity(pos) as? BlockEntityCore ?: continue
                val host = (listOf<Direction?>(null) + Direction.entries).firstNotNullOfOrNull { be.getModule(Modules.MENU, it) } ?: continue
                BlockEntityContents.fill(be)
                stock(player)
                val opened = host.createMenu(1, player.inventory, player)
                if (opened == null) {
                    problems += "$name offers a menu but creates none"
                    continue
                }
                menu = opened
                opened.broadcastChanges()
                val start = total(opened, level, pos)
                slots@ for (slot in opened.slots.indices) {
                    for ((click, run) in CLICKS) {
                        try {
                            run(opened, slot, player)
                            opened.broadcastChanges()
                        } catch (e: Exception) {
                            problems += "$name: $click on slot $slot threw ${e::class.java.simpleName}: ${e.message}"
                            continue
                        }
                        val now = total(opened, level, pos)
                        if (now != start) {
                            problems += "$name: $click on slot $slot changed the items in play: ${diff(start, now)}"
                            break@slots // every later total would differ too
                        }
                    }
                }
                opened.removed(player)
                val end = total(opened, level, pos)
                if (problems.none { it.startsWith("$name: ") } && end != start) problems += "$name: closing the menu changed the items in play: ${diff(start, end)}"
            } catch (e: Exception) {
                problems += "$name threw ${e::class.java.simpleName}: ${e.message}"
            } finally {
                level.getEntitiesOfClass(ItemEntity::class.java, AABB(pos).inflate(6.0)).forEach { it.discard() }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)
            }
        }
        return problems
    }

    private fun stock(player: ServerPlayer) {
        player.inventory.clearContent()
        player.containerMenu.setCarried(ItemStack.EMPTY)
        val items = listOf(Items.DIAMOND, Items.GOLD_INGOT, Items.STICK, Items.IRON_INGOT, Items.DIRT)
        for (i in 0 until 14) player.inventory.setItem(i, ItemStack(items[i % items.size], 3 + i))
    }

    private fun total(menu: AbstractContainerMenu, level: ServerLevel, pos: BlockPos): BlockEntityContents.Tally {
        val counts = TreeMap<String, Long>()
        fun add(stack: ItemStack) {
            if (!stack.isEmpty) counts.merge(BlockEntityContents.itemKey(stack), stack.count.toLong(), Long::plus)
        }
        menu.slots.forEach { add(it.item) }
        add(menu.carried)
        for (e in level.getEntitiesOfClass(ItemEntity::class.java, AABB(pos).inflate(6.0))) add(e.item)
        return BlockEntityContents.Tally(counts)
    }

    private fun diff(a: BlockEntityContents.Tally, b: BlockEntityContents.Tally): String {
        val lost = a.missingFrom(b)
        val gained = b.missingFrom(a)
        return buildList {
            if (lost.isNotEmpty()) add("lost $lost")
            if (gained.isNotEmpty()) add("gained $gained")
        }.joinToString("; ")
    }
}
