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
import net.minecraft.world.entity.player.Player
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

    /** Prerequisites of [id] the team has not researched (unknown ids included). */
    fun missingPrerequisites(id: Identifier, research: Research): List<Identifier> =
        all[id]?.prerequisites?.filterNot { all.containsKey(it) && isResearched(it, research) } ?: emptyList()

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

    companion object {
        @JvmField
        val EMPTY = Technologies(emptyMap())
    }
}

/**
 * Tech trees: technologies loaded from datapacks into the [KEY] registry, synced to clients, and researched per team.
 * A team's research is [Research] team data (shared by its members; a solo player's team is theirs alone). Mods decide
 * what produces research progress (a machine, an item, an advancement) and pass it to [addProgress]; the technology
 * unlocks when its progress reaches its cost.
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
     * not used. Reaching the cost unlocks the technology and posts [TechnologyResearchedEvent]. Server thread only.
     *
     * @return The progress used: between 0 and [amount].
     */
    @JvmStatic
    fun addProgress(server: MinecraftServer, team: UUID, technology: Identifier, amount: Long): Long {
        if (amount < 0L) return 0L
        val techs = of(server.registryAccess())
        val tech = techs[technology] ?: return 0L
        val research = ItszuLib.TEAMS.state.team(team)?.get(Research.TYPE) ?: return 0L
        if (techs.state(technology, research) != TechnologyState.AVAILABLE) return 0L
        val have = research.progressOf(technology)
        val used = minOf(amount, tech.cost - have)
        val unlocks = have + used >= tech.cost
        if (used <= 0L && !unlocks) return 0L
        ItszuLib.TEAMS.change { state ->
            state.update(team, Research.TYPE) { if (unlocks) it.unlock(technology) else it.withProgress(technology, have + used) }
        }
        if (unlocks) NeoForge.EVENT_BUS.post(TechnologyResearchedEvent(server, team, technology))
        return used
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
        NeoForge.EVENT_BUS.post(TechnologyResearchedEvent(server, team, technology))
        return true
    }
}

/**
 * Posted on the game bus (server thread) after a team unlocks a technology through [TechTree.addProgress] or
 * [TechTree.unlock].
 */
class TechnologyResearchedEvent(val server: MinecraftServer, val team: UUID, val technology: Identifier) : Event()
