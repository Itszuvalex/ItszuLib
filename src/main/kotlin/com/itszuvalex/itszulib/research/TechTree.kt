package com.itszuvalex.itszulib.research

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.team.ClientTeam
import com.itszuvalex.itszulib.team.Research
import com.mojang.logging.LogUtils
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.team.TeamMembershipChangedEvent
import net.neoforged.neoforge.common.crafting.SizedIngredient
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.registries.DataPackRegistryEvent
import java.util.UUID

/**
 * A set of technologies, usually every one in the [TechTree.KEY] registry ([TechTree.of]). Pure: the rules for
 * what a team can research live here, testable without a game.
 */
class Technologies(val all: Map<Identifier, Technology>) {
    operator fun get(id: Identifier): Technology? = all[id]

    /** The trees with at least one technology. */
    val trees: Set<Identifier> by lazy { all.values.mapTo(sortedSetOf()) { it.tree } }

    fun inTree(tree: Identifier): Map<Identifier, Technology> = all.filterValues { it.tree == tree }

    private val layouts = HashMap<Identifier, TechTreeLayout.Result>()

    /** [tree]'s layout, computed once. */
    fun layout(tree: Identifier): TechTreeLayout.Result = synchronized(layouts) { layouts.getOrPut(tree) { TechTreeLayout.layout(inTree(tree)) } }

    /**
     * Researched by the team, or unlocked by default. An id with no technology counts only if the team has it.
     */
    fun isResearched(id: Identifier, research: Research): Boolean = research.has(id) || all[id]?.unlockedByDefault == true

    /**
     * @return Null for an unknown id.
     */
    fun state(id: Identifier, research: Research): TechnologyState? {
        val tech = all[id] ?: return null
        return when {
            isResearched(id, research) -> TechnologyState.RESEARCHED
            tech.prerequisites.all { all.containsKey(it) && isResearched(it, research) } -> TechnologyState.AVAILABLE
            tech.hidden -> TechnologyState.HIDDEN
            else -> TechnologyState.LOCKED
        }
    }

    /**
     * What [id] still needs from the team: progress points, each resource and each item (with how many are still to
     * be delivered). Null for an unknown id.
     */
    fun remaining(id: Identifier, research: Research): Remaining? {
        val tech = all[id] ?: return null
        if (isResearched(id, research)) return Remaining(0L, emptyMap(), tech.items.map { it to 0 })
        return Remaining(
            (tech.cost - research.progressOf(id)).coerceAtLeast(0L),
            tech.resources.mapValues { (r, amount) -> (amount - research.requirementOf(id, resourceKey(r))).coerceAtLeast(0L) },
            tech.items.mapIndexed { i, item -> item to (item.count() - research.requirementOf(id, itemKey(i)).toInt()).coerceAtLeast(0) },
        )
    }

    /**
     * Hands in what [id] still needs of its items from [stacks], shrinking them, and returns the updated research and
     * how many items were taken. [matches] decides whether a stack counts for a requirement ([matchesItem] by
     * default). Pure: callers store the result ([TechTree.deliver]).
     */
    @JvmOverloads
    fun deliver(
        id: Identifier,
        research: Research,
        stacks: Iterable<IItemStack>,
        matches: (SizedIngredient, IItemStack) -> Boolean = ::matchesItem,
    ): Pair<Research, Int> {
        val tech = all[id] ?: return research to 0
        if (isResearched(id, research)) return research to 0
        var updated = research
        var taken = 0
        tech.items.forEachIndexed { index, item ->
            val key = itemKey(index)
            var have = updated.requirementOf(id, key)
            for (stack in stacks) {
                val left = item.count() - have
                if (left <= 0L) break
                if (stack.isEmpty() || !matches(item, stack)) continue
                val take = minOf(left, stack.stackSize().toLong()).toInt()
                stack.modifyStackSize(-take)
                have += take
                taken += take
            }
            updated = updated.withRequirement(id, key, have)
        }
        return updated to taken
    }

    /** Whether every requirement of [id] is met (its cost, resources and items). */
    fun requirementsMet(id: Identifier, research: Research): Boolean = remaining(id, research)?.complete == true

    /**
     * Researched technologies with rewards that [player] has not had yet, in id order.
     */
    fun unclaimedRewards(research: Research, player: UUID): List<Identifier> =
        research.unlocked.filter { all[it]?.rewards?.isNotEmpty() == true && !research.hasClaimed(it, player) }.sorted()

    /** Prerequisites of [id] the team has not researched (unknown ids included). */
    fun missingPrerequisites(id: Identifier, research: Research): List<Identifier> =
        all[id]?.prerequisites?.filterNot { all.containsKey(it) && isResearched(it, research) } ?: emptyList()

    /**
     * What to research to get [id]: its unresearched prerequisites, all the way down, each after its own
     * prerequisites, then [id] itself. Empty if [id] is researched, unknown, or can never become available (an
     * unknown prerequisite, or a cycle).
     */
    fun pathTo(id: Identifier, research: Research): List<Identifier> {
        val path = LinkedHashSet<Identifier>()
        val visiting = HashSet<Identifier>()
        fun visit(t: Identifier): Boolean {
            if (isResearched(t, research) || t in path) return true
            val tech = all[t] ?: return false
            if (!visiting.add(t)) return false
            if (!tech.prerequisites.all(::visit)) return false
            visiting -= t
            path += t
            return true
        }
        return if (visit(id)) path.toList() else emptyList()
    }

    /**
     * The team's focus in [tree]: the first queued technology of that tree it can research now.
     */
    fun focus(tree: Identifier, research: Research): Identifier? =
        research.queue.firstOrNull { all[it]?.tree == tree && state(it, research) == TechnologyState.AVAILABLE }

    /**
     * [research] with [id] off the queue, and with it every queued technology that needs it (directly or through
     * others), since they could not be researched before it.
     */
    fun unqueue(id: Identifier, research: Research): Research {
        val removed = HashSet<Identifier>().apply { add(id) }
        fun needs(t: Identifier, seen: MutableSet<Identifier> = HashSet()): Boolean {
            if (!seen.add(t)) return false
            return all[t]?.prerequisites.orEmpty().any { (it in removed && !isResearched(it, research)) || needs(it, seen) }
        }
        for (t in research.queue) if (t != id && needs(t)) removed += t
        return research.unqueue(removed)
    }

    /**
     * Datapack mistakes: prerequisites that do not exist (the technology can never become available) and
     * prerequisite cycles (neither can any technology on one). Empty when the tree is sound.
     */
    fun problems(): List<String> {
        val problems = ArrayList<String>()
        for ((id, tech) in all.toSortedMap()) {
            for (p in tech.prerequisites) if (!all.containsKey(p)) problems += "Technology $id requires unknown technology $p"
        }
        // Cycles: depth first, reporting each cycle once by the edge that closes it.
        val done = HashSet<Identifier>()
        val stack = LinkedHashSet<Identifier>()
        fun visit(id: Identifier) {
            if (id in done) return
            stack += id
            for (p in all[id]?.prerequisites.orEmpty().filter(all::containsKey).sorted()) {
                if (p in stack) problems += "Technology prerequisite cycle: ${(stack.dropWhile { it != p } + p).joinToString(" -> ")}"
                else visit(p)
            }
            stack -= id
            done += id
        }
        all.keys.sorted().forEach(::visit)
        return problems
    }

    /**
     * What a technology still needs ([remaining]).
     *
     * @param items Each required item and how many are still to be delivered.
     */
    data class Remaining(val points: Long, val resources: Map<Identifier, Long>, val items: List<Pair<SizedIngredient, Int>>) {
        val complete: Boolean get() = points <= 0L && resources.values.all { it <= 0L } && items.all { it.second <= 0 }
    }

    companion object {
        @JvmField
        val EMPTY = Technologies(emptyMap())

        /**
         * Whether [stack] counts for [item]: by item id for a plain ingredient (items or a tag; no registry lookup or
         * vanilla stack needed), through the vanilla stack for a custom ingredient (components and the like).
         */
        @JvmStatic
        fun matchesItem(item: SizedIngredient, stack: IItemStack): Boolean {
            val ingredient = item.ingredient()
            if (ingredient.isCustom) return ingredient.test(stack.toMinecraft())
            return ingredient.items().anyMatch { holder -> holder.unwrapKey().map { it.identifier() == stack.item() }.orElse(false) }
        }

        /** The [Research.requirements] key of resource [resource]. */
        @JvmStatic
        fun resourceKey(resource: Identifier): String = "resource/$resource"

        /** The [Research.requirements] key of a technology's item requirement [index]. */
        @JvmStatic
        fun itemKey(index: Int): String = "item/$index"

        /** A resource's display name: the translation key `research_resource.<namespace>.<path>`. */
        @JvmStatic
        fun resourceName(resource: Identifier): Component = Component.translatable("research_resource.${resource.namespace}.${resource.path.replace('/', '.')}")
    }
}

/**
 * Tech trees: technologies loaded from datapacks into the [KEY] registry, synced to clients, and researched per team.
 * A team's research is [Research] team data (shared by its members; a solo player's team is theirs alone). Mods decide
 * what produces research progress (a machine, an item, an advancement) and pass it to [addProgress]; technologies may
 * also need other resources ([addResource]) and items ([deliver]), and unlock when every requirement is met, handing
 * out their rewards ([claimRewards]). Teams also keep a queue of what to research next ([queue], [unqueue]);
 * a machine that researches for a team usually works on its [focus].
 *
 * Changes go through [ItszuLib.TEAMS], which syncs the team to its members, so add progress in batches (say once a
 * second) rather than every tick.
 */
object TechTree {
    @JvmField
    val KEY: ResourceKey<Registry<Technology>> = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(ItszuLib.ID, "technology"))

    private val LOGGER = LogUtils.getLogger()

    @JvmStatic
    fun register(modBus: IEventBus) {
        modBus.addListener { event: DataPackRegistryEvent.NewRegistry -> event.dataPackRegistry(KEY, Technology.CODEC, Technology.CODEC) }
        NeoForge.EVENT_BUS.addListener { event: ServerStartedEvent ->
            of(event.server.registryAccess()).problems().forEach { LOGGER.error("Tech tree: {}", it) }
        }
        NeoForge.EVENT_BUS.addListener { event: PlayerEvent.PlayerLoggedInEvent -> (event.entity as? ServerPlayer)?.let(::claimRewards) }
        NeoForge.EVENT_BUS.addListener { event: TeamMembershipChangedEvent -> event.server.playerList.getPlayer(event.player)?.let(::claimRewards) }
    }

    private var cachedRegistry: Registry<Technology>? = null
    private var cached: Technologies = Technologies.EMPTY

    /**
     * The technologies in [access] (the server's, or a client level's). Rebuilt only when the registry changes.
     */
    @JvmStatic
    fun of(access: RegistryAccess): Technologies {
        val registry = access.lookup(KEY).orElse(null) ?: return Technologies.EMPTY
        synchronized(this) {
            if (registry !== cachedRegistry) {
                cached = Technologies(registry.entrySet().associate { it.key.identifier() to it.value })
                cachedRegistry = registry
            }
            return cached
        }
    }

    /**
     * [player]'s team's research: from the server's teams, or on the client from the synced copy of the local
     * player's own team (other players' research is not synced; they read as empty).
     */
    @JvmStatic
    fun research(player: Player): Research {
        val team = if (player.level().isClientSide) ClientTeam.current?.takeIf { player.uuid in it.members } else ItszuLib.TEAMS.state.teamOf(player.uuid)
        return team?.get(Research.TYPE) ?: Research.EMPTY
    }

    /**
     * Whether [player]'s team has researched [id] (or it is unlocked by default): the check for gating recipes,
     * blocks and actions. Works on both sides (see [research]).
     */
    @JvmStatic
    fun isResearched(player: Player, id: Identifier): Boolean = of(player.level().registryAccess()).isResearched(id, research(player))

    /**
     * Adds up to [amount] progress towards [technology] for [team]. Nothing happens unless the technology exists and
     * is [TechnologyState.AVAILABLE] to the team. Progress past the cost is not taken, so a machine can keep what was
     * not used. Meeting every requirement (the cost, and any resources and items) unlocks the technology, posts
     * [TechnologyResearchedEvent] and hands out its rewards. Server thread only.
     *
     * @return The progress used: between 0 and [amount].
     */
    @JvmStatic
    fun addProgress(server: MinecraftServer, team: UUID, technology: Identifier, amount: Long): Long {
        if (amount < 0L) return 0L
        return contribute(server, team, technology) { tech, research ->
            val have = research.progressOf(technology)
            val used = minOf(amount, tech.cost - have).coerceAtLeast(0L)
            research.withProgress(technology, have + used) to used
        }
    }

    /**
     * Adds up to [amount] of [resource] towards [technology] for [team] (see [Technology.resources]); as
     * [addProgress], but for a technology that needs [resource].
     *
     * @return The amount used: between 0 and [amount].
     */
    @JvmStatic
    fun addResource(server: MinecraftServer, team: UUID, technology: Identifier, resource: Identifier, amount: Long): Long {
        if (amount < 0L) return 0L
        return contribute(server, team, technology) { tech, research ->
            val need = tech.resources[resource] ?: return@contribute null
            val key = Technologies.resourceKey(resource)
            val have = research.requirementOf(technology, key)
            val used = minOf(amount, need - have).coerceAtLeast(0L)
            research.withRequirement(technology, key, have + used) to used
        }
    }

    /**
     * Hands in what [technology] still needs of its items ([Technology.items]) from [stacks], shrinking them (a
     * player's inventory, a machine's storage: mark them changed afterwards). As [addProgress] otherwise.
     *
     * @return How many items were taken.
     */
    @JvmStatic
    fun deliver(server: MinecraftServer, team: UUID, technology: Identifier, stacks: Iterable<IItemStack>): Int =
        contribute(server, team, technology) { _, research ->
            val (updated, taken) = of(server.registryAccess()).deliver(technology, research, stacks)
            updated to taken.toLong()
        }.toInt()

    /**
     * [deliver] from vanilla stacks (shrunk in place), such as `player.inventory.nonEquipmentItems`.
     */
    @JvmStatic
    fun deliverFrom(server: MinecraftServer, team: UUID, technology: Identifier, stacks: Iterable<ItemStack>): Int =
        deliver(server, team, technology, stacks.map(IItemStack::of))

    /**
     * Applies [change] (the updated research and how much it used, or null for nothing to do) to [team]'s research
     * of [technology] if the technology is available, unlocking it once every requirement is met.
     */
    private fun contribute(server: MinecraftServer, team: UUID, technology: Identifier, change: (Technology, Research) -> Pair<Research, Long>?): Long {
        val techs = of(server.registryAccess())
        val tech = techs[technology] ?: return 0L
        val research = ItszuLib.TEAMS.state.team(team)?.get(Research.TYPE) ?: return 0L
        if (techs.state(technology, research) != TechnologyState.AVAILABLE) return 0L
        val (updated, used) = change(tech, research) ?: return 0L
        val unlocks = techs.requirementsMet(technology, updated)
        if (used <= 0L && !unlocks) return 0L
        ItszuLib.TEAMS.change { state -> state.update(team, Research.TYPE) { if (unlocks) updated.unlock(technology) else updated } }
        if (unlocks) researched(server, team, technology)
        return used
    }

    private fun researched(server: MinecraftServer, team: UUID, technology: Identifier) {
        NeoForge.EVENT_BUS.post(TechnologyResearchedEvent(server, team, technology))
        val members = ItszuLib.TEAMS.state.team(team)?.members?.keys ?: return
        for (player in server.playerList.players) if (player.uuid in members) claimRewards(player)
    }

    /**
     * Gives [player] the rewards of every technology their team has researched that they have not had yet (into
     * their inventory, dropped if it is full), and records them. Called when a technology is researched (for members
     * online), when a player logs in and when they change team. Server thread only.
     *
     * @return How many technologies' rewards were given.
     */
    @JvmStatic
    fun claimRewards(player: ServerPlayer): Int {
        val server = player.level().server
        val team = ItszuLib.TEAMS.state.teamOf(player.uuid) ?: return 0
        val research = team[Research.TYPE]
        val techs = of(server.registryAccess())
        val ids = techs.unclaimedRewards(research, player.uuid)
        if (ids.isEmpty()) return 0
        ItszuLib.TEAMS.change { state -> state.update(team.id, Research.TYPE) { it.withClaimed(player.uuid, ids) } }
        for (id in ids) for (reward in techs[id]!!.rewards) {
            val stack = reward.create()
            if (!player.inventory.add(stack) && !stack.isEmpty) player.drop(stack, false)
        }
        return ids.size
    }

    /**
     * Queues [technology] for [team], after whatever unresearched prerequisites it needs that are not queued yet
     * ([Technologies.pathTo]). Server thread only.
     *
     * @return True if the queue changed.
     */
    @JvmStatic
    fun queue(server: MinecraftServer, team: UUID, technology: Identifier): Boolean {
        val research = ItszuLib.TEAMS.state.team(team)?.get(Research.TYPE) ?: return false
        val updated = research.enqueue(of(server.registryAccess()).pathTo(technology, research))
        if (updated == research) return false
        ItszuLib.TEAMS.change { state -> state.update(team, Research.TYPE) { updated } }
        return true
    }

    /**
     * Takes [technology] off [team]'s queue, with the queued technologies that need it ([Technologies.unqueue]).
     * Server thread only.
     *
     * @return True if the queue changed.
     */
    @JvmStatic
    fun unqueue(server: MinecraftServer, team: UUID, technology: Identifier): Boolean {
        val research = ItszuLib.TEAMS.state.team(team)?.get(Research.TYPE) ?: return false
        val updated = of(server.registryAccess()).unqueue(technology, research)
        if (updated == research) return false
        ItszuLib.TEAMS.change { state -> state.update(team, Research.TYPE) { updated } }
        return true
    }

    /**
     * [team]'s focus in [tree] ([Technologies.focus]): what a machine researching for the team should work on.
     */
    @JvmStatic
    fun focus(server: MinecraftServer, team: UUID, tree: Identifier): Identifier? {
        val research = ItszuLib.TEAMS.state.team(team)?.get(Research.TYPE) ?: return null
        return of(server.registryAccess()).focus(tree, research)
    }

    /**
     * Unlocks [technology] for [team] whatever its prerequisites and progress (commands, rewards). Posts
     * [TechnologyResearchedEvent] if it was not already unlocked. Server thread only.
     *
     * @return True if it was newly unlocked.
     */
    @JvmStatic
    fun unlock(server: MinecraftServer, team: UUID, technology: Identifier): Boolean {
        val research = ItszuLib.TEAMS.state.team(team)?.get(Research.TYPE) ?: return false
        if (research.has(technology)) return false
        ItszuLib.TEAMS.change { state -> state.update(team, Research.TYPE) { it.unlock(technology) } }
        researched(server, team, technology)
        return true
    }
}

/**
 * Posted on the game bus (server thread) after a team unlocks a technology through [TechTree.addProgress] or
 * [TechTree.unlock].
 */
class TechnologyResearchedEvent(val server: MinecraftServer, val team: UUID, val technology: Identifier) : Event()
