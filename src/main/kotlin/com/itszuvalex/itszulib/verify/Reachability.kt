package com.itszuvalex.itszulib.verify

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.display.SlotDisplayContext

/**
 * What can be obtained starting from some base items, by repeatedly applying everything that makes things from other
 * things (recipes, machines, drops). A way to find items nothing can make, or that can only be made from themselves or
 * from each other, whatever the balance of the game. Pure ([reachable] and [blockedBy] need no game), so it is
 * unit-tested; [recipeProducers] reads the server's crafting recipes.
 */
object Reachability {
    /**
     * Something that makes [makes] when every one of [needs] is met, each need by any one of the items in the set
     * (a recipe slot's alternatives). No needs means it can always run.
     *
     * @param source What it is, for reports.
     */
    data class Producer<K : Any>(val source: String, val needs: List<Set<K>>, val makes: Set<K>)

    /**
     * Everything obtainable from [base] through [producers], by running every producer whose needs are met until
     * nothing new appears.
     */
    @JvmStatic
    fun <K : Any> reachable(base: Set<K>, producers: List<Producer<K>>): Set<K> {
        val reached = HashSet(base)
        var changed = true
        while (changed) {
            changed = false
            for (p in producers) {
                if (p.makes.all { it in reached }) continue
                if (p.needs.all { need -> need.any { it in reached } }) changed = reached.addAll(p.makes) || changed
            }
        }
        return reached
    }

    /**
     * Why [target] is not in [reached]: each producer that makes it, with what it still needs, or a note that nothing
     * makes it at all.
     */
    @JvmStatic
    fun <K : Any> blockedBy(target: K, reached: Set<K>, producers: List<Producer<K>>): List<String> {
        val makers = producers.filter { target in it.makes }
        if (makers.isEmpty()) return listOf("nothing makes it")
        return makers.map { p ->
            val unmet = p.needs.filter { need -> need.none { it in reached } }
            "${p.source} needs ${unmet.joinToString(" and ") { n -> n.take(3).joinToString(" or ") + if (n.size > 3) " or ${n.size - 3} more" else "" }}"
        }
    }

    /**
     * The server's crafting, smelting, stonecutting and smithing recipes as producers of items: each ingredient slot
     * is a need (any of the items the ingredient matches), and the recipe's result is what it makes. Recipes whose
     * result or ingredients cannot be listed are left out.
     */
    @JvmStatic
    fun recipeProducers(level: ServerLevel): List<Producer<Item>> {
        val context = SlotDisplayContext.fromLevel(level)
        val producers = ArrayList<Producer<Item>>()
        for (holder in level.recipeAccess().getRecipes()) {
            val recipe = holder.value()
            val needs = recipe.placementInfo().ingredients().map { ing -> ing.items().toList().map { it.value() }.toSet() }
            val makes = recipe.display().flatMap { d -> d.result().resolveForStacks(context) }.filter { !it.isEmpty }.map { it.item }.toSet()
            if (makes.isEmpty() || needs.any { it.isEmpty() }) continue
            producers += Producer(holder.id().identifier().toString(), needs, makes)
        }
        return producers
    }

    /** Every registered item in [namespace]. */
    @JvmStatic
    fun itemsOf(namespace: String): Set<Item> =
        BuiltInRegistries.ITEM.entrySet().filter { it.key.identifier().namespace == namespace }.map { it.value }.toSet()
}
