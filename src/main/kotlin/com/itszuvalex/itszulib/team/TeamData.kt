package com.itszuvalex.itszulib.team

import com.itszuvalex.itszulib.ItszuLib
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.Identifier
import org.jetbrains.annotations.TestOnly
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * A kind of data every team holds, such as [Research]. Values are immutable: changing one means storing a new value.
 *
 * @param codec Persists the value. Decoding a saved value that this codec rejects fails the whole load (see
 * [TeamStore]) rather than silently dropping it.
 * @param empty The value of a team that has none yet (e.g. a new player's solo team).
 * @param merge Used when a player joins a team: `merge(team's value, joining player's value)`.
 * @param copy Used when a player leaves or is removed: the value they take into their new solo team. Defaults to the
 * same value, which is safe because values are immutable.
 */
class TeamDataType<T : Any> @JvmOverloads constructor(
    val id: Identifier,
    val codec: Codec<T>,
    val empty: () -> T,
    val merge: (team: T, joining: T) -> T,
    val copy: (T) -> T = { it },
) {
    override fun toString(): String = "TeamDataType[$id]"
}

/**
 * Registry of [TeamDataType]s. Register during mod construction, before any server starts: saved data of an id with
 * no registered type is kept as raw data (see [Team.unknownData]), not decoded.
 */
object TeamDataTypes {
    private val TYPES = ConcurrentHashMap<Identifier, TeamDataType<*>>()

    /**
     * @throws IllegalArgumentException if a type with this id is already registered.
     */
    @JvmStatic
    fun <T : Any> register(type: TeamDataType<T>): TeamDataType<T> {
        require(TYPES.putIfAbsent(type.id, type) == null) { "Team data type ${type.id} already registered." }
        return type
    }

    @JvmStatic
    fun byId(id: Identifier): TeamDataType<*>? = TYPES[id]

    @JvmStatic
    fun all(): Collection<TeamDataType<*>> = TYPES.values

    @TestOnly
    @JvmStatic
    fun clear() = TYPES.clear()
}

/**
 * The research a team has unlocked, its partial progress towards technologies not yet unlocked, and the queue of what
 * it wants researched next (see [com.itszuvalex.itszulib.research.TechTree]). Unlocks and progress only grow: joining a
 * team unions research and keeps the larger progress, leaving copies it. The team's queue comes first when joining.
 *
 * @param progress Positive amounts only, and never for an unlocked id.
 * @param queue Technologies to research next, in order: no duplicates and nothing unlocked. Machines usually work on
 * the first one they can ([com.itszuvalex.itszulib.research.Technologies.focus]).
 * @param requirements Progress on a technology's other requirements, by requirement key
 * ([com.itszuvalex.itszulib.research.Technologies.resourceKey], [com.itszuvalex.itszulib.research.Technologies.itemKey]):
 * positive amounts only, never for an unlocked id.
 * @param claimed Players who got a researched technology's rewards (unlocked ids only). Claims travel with players:
 * joining unions them and leaving copies them, so a player gets each reward once.
 */
data class Research @JvmOverloads constructor(
    val unlocked: Set<Identifier>,
    val progress: Map<Identifier, Long> = emptyMap(),
    val queue: List<Identifier> = emptyList(),
    val requirements: Map<Identifier, Map<String, Long>> = emptyMap(),
    val claimed: Map<Identifier, Set<UUID>> = emptyMap(),
) {
    init {
        problem(unlocked, progress, queue, requirements, claimed)?.let { throw IllegalArgumentException(it) }
    }

    fun has(id: Identifier): Boolean = id in unlocked

    fun progressOf(id: Identifier): Long = progress[id] ?: 0L

    /** Progress on requirement [key] of [id]. */
    fun requirementOf(id: Identifier, key: String): Long = requirements[id]?.get(key) ?: 0L

    /**
     * Unlocks [id], dropping its progress and taking it off the queue.
     */
    fun unlock(id: Identifier): Research = if (has(id)) this else copy(unlocked = unlocked + id, progress = progress - id, queue = queue - id, requirements = requirements - id)

    /**
     * Sets the progress towards [id] (an unlocked id keeps none; 0 or less clears it).
     */
    fun withProgress(id: Identifier, amount: Long): Research = when {
        has(id) -> this
        amount <= 0L -> if (id in progress) copy(progress = progress - id) else this
        else -> copy(progress = progress + (id to amount))
    }

    /**
     * Sets the progress on requirement [key] of [id] (an unlocked id keeps none; 0 or less clears it).
     */
    fun withRequirement(id: Identifier, key: String, amount: Long): Research {
        if (has(id)) return this
        val current = requirements[id].orEmpty()
        val updated = if (amount <= 0L) current - key else current + (key to amount)
        if (updated == current) return this
        return copy(requirements = if (updated.isEmpty()) requirements - id else requirements + (id to updated))
    }

    /** Whether [player] got [id]'s rewards. */
    fun hasClaimed(id: Identifier, player: UUID): Boolean = player in claimed[id].orEmpty()

    /**
     * Records that [player] got the rewards of [ids] (unlocked ones only).
     */
    fun withClaimed(player: UUID, ids: Collection<Identifier>): Research {
        val add = ids.filter { has(it) && !hasClaimed(it, player) }
        if (add.isEmpty()) return this
        return copy(claimed = claimed + add.associateWith { claimed[it].orEmpty() + player })
    }

    /**
     * Replaces the queue, dropping duplicates (the first stays) and unlocked ids.
     */
    fun withQueue(ids: List<Identifier>): Research = copy(queue = ids.distinct().filterNot(::has))

    /**
     * Appends [ids] not already queued or unlocked, in order.
     */
    fun enqueue(ids: List<Identifier>): Research = withQueue(queue + ids)

    /**
     * Takes [ids] off the queue.
     */
    fun unqueue(ids: Collection<Identifier>): Research = if (ids.none { it in queue }) this else copy(queue = queue - ids.toSet())

    /** 0-based place of [id] in the queue, or -1. */
    fun queuePosition(id: Identifier): Int = queue.indexOf(id)

    companion object {
        @JvmField
        val EMPTY = Research(emptySet())

        private fun problem(
            unlocked: Set<Identifier>,
            progress: Map<Identifier, Long>,
            queue: List<Identifier>,
            requirements: Map<Identifier, Map<String, Long>>,
            claimed: Map<Identifier, Set<UUID>>,
        ): String? = when {
            progress.values.any { it <= 0L } -> "Research progress must be positive: $progress"
            progress.keys.any { it in unlocked } -> "Research progress kept for unlocked research: ${progress.keys.filter { it in unlocked }}"
            queue.size != queue.toSet().size -> "Research queue has duplicates: $queue"
            queue.any { it in unlocked } -> "Research queue holds unlocked research: ${queue.filter { it in unlocked }}"
            requirements.values.any { m -> m.isEmpty() || m.values.any { it <= 0L } } -> "Research requirement progress must be positive: $requirements"
            requirements.keys.any { it in unlocked } -> "Research requirement progress kept for unlocked research: ${requirements.keys.filter { it in unlocked }}"
            claimed.keys.any { it !in unlocked } -> "Rewards claimed for research not unlocked: ${claimed.keys.filter { it !in unlocked }}"
            claimed.values.any { it.isEmpty() } -> "Empty reward claims: $claimed"
            else -> null
        }

        private data class Fields(
            val unlocked: List<Identifier>,
            val progress: Map<Identifier, Long>,
            val queue: List<Identifier>,
            val requirements: Map<Identifier, Map<String, Long>>,
            val claimed: Map<Identifier, List<UUID>>,
        )

        private val RECORD: Codec<Research> = RecordCodecBuilder.create<Fields> { i ->
            i.group(
                Identifier.CODEC.listOf().optionalFieldOf("unlocked", emptyList()).forGetter(Fields::unlocked),
                Codec.unboundedMap(Identifier.CODEC, Codec.LONG).optionalFieldOf("progress", emptyMap()).forGetter(Fields::progress),
                Identifier.CODEC.listOf().optionalFieldOf("queue", emptyList()).forGetter(Fields::queue),
                Codec.unboundedMap(Identifier.CODEC, Codec.unboundedMap(Codec.STRING, Codec.LONG)).optionalFieldOf("requirements", emptyMap()).forGetter(Fields::requirements),
                Codec.unboundedMap(Identifier.CODEC, UUIDUtil.CODEC.listOf()).optionalFieldOf("claimed", emptyMap()).forGetter(Fields::claimed),
            ).apply(i, ::Fields)
        }.comapFlatMap(
            { f ->
                val set = f.unlocked.toSet()
                val claimed = f.claimed.mapValues { it.value.toSet() }
                problem(set, f.progress, f.queue, f.requirements, claimed)?.let { DataResult.error { it } }
                    ?: DataResult.success(Research(set, f.progress, f.queue, f.requirements, claimed))
            },
            { r ->
                Fields(r.unlocked.sorted(), r.progress.toSortedMap(), r.queue, r.requirements.mapValues { it.value.toSortedMap() }.toSortedMap(),
                    r.claimed.mapValues { it.value.sorted() }.toSortedMap())
            },
        )

        /** Saves from before progress existed: a plain list of unlocked ids. */
        private val UNLOCKED_ONLY: Codec<Research> = Identifier.CODEC.listOf().xmap({ Research(it.toSet()) }, { it.unlocked.sorted() })

        @JvmField
        val CODEC: Codec<Research> = Codec.withAlternative(RECORD, UNLOCKED_ONLY)

        /**
         * Unions unlocks and reward claims, keeps the larger progress (on cost and on each requirement), and queues
         * the team's queue then the joiner's additions.
         */
        @JvmStatic
        fun merge(team: Research, joining: Research): Research {
            val unlocked = team.unlocked + joining.unlocked
            val progress = HashMap<Identifier, Long>()
            for ((id, amount) in team.progress.entries + joining.progress.entries) {
                if (id !in unlocked) progress.merge(id, amount, ::maxOf)
            }
            val requirements = HashMap<Identifier, Map<String, Long>>()
            for ((id, map) in team.requirements.entries + joining.requirements.entries) {
                if (id in unlocked) continue
                requirements[id] = (requirements[id].orEmpty().keys + map.keys).associateWith { maxOf(requirements[id]?.get(it) ?: 0L, map[it] ?: 0L) }
            }
            val claimed = HashMap<Identifier, Set<UUID>>()
            for ((id, players) in team.claimed.entries + joining.claimed.entries) claimed[id] = claimed[id].orEmpty() + players
            return Research(unlocked, progress, (team.queue + joining.queue).distinct().filterNot(unlocked::contains), requirements, claimed)
        }

        @JvmField
        val TYPE: TeamDataType<Research> = TeamDataType(
            Identifier.fromNamespaceAndPath(ItszuLib.ID, "research"),
            CODEC,
            { EMPTY },
            ::merge,
        )
    }
}
