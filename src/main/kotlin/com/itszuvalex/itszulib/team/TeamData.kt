package com.itszuvalex.itszulib.team

import com.itszuvalex.itszulib.ItszuLib
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.Identifier
import org.jetbrains.annotations.TestOnly
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
 * The research a team has unlocked, and its partial progress towards technologies not yet unlocked (see
 * [com.itszuvalex.itszulib.research.TechTree]). Only grows: joining a team unions research and keeps the larger
 * progress, leaving copies it.
 *
 * @param progress Positive amounts only, and never for an unlocked id.
 */
data class Research @JvmOverloads constructor(
    val unlocked: Set<Identifier>,
    val progress: Map<Identifier, Long> = emptyMap(),
) {
    init {
        problem(unlocked, progress)?.let { throw IllegalArgumentException(it) }
    }

    fun has(id: Identifier): Boolean = id in unlocked

    fun progressOf(id: Identifier): Long = progress[id] ?: 0L

    fun unlock(id: Identifier): Research = if (has(id)) this else Research(unlocked + id, progress - id)

    /**
     * Sets the progress towards [id] (an unlocked id keeps none; 0 or less clears it).
     */
    fun withProgress(id: Identifier, amount: Long): Research = when {
        has(id) -> this
        amount <= 0L -> if (id in progress) Research(unlocked, progress - id) else this
        else -> Research(unlocked, progress + (id to amount))
    }

    companion object {
        @JvmField
        val EMPTY = Research(emptySet())

        private fun problem(unlocked: Set<Identifier>, progress: Map<Identifier, Long>): String? = when {
            progress.values.any { it <= 0L } -> "Research progress must be positive: $progress"
            progress.keys.any { it in unlocked } -> "Research progress kept for unlocked research: ${progress.keys.filter { it in unlocked }}"
            else -> null
        }

        private val RECORD: Codec<Research> = RecordCodecBuilder.create<Pair<List<Identifier>, Map<Identifier, Long>>> { i ->
            i.group(
                Identifier.CODEC.listOf().optionalFieldOf("unlocked", emptyList()).forGetter { it.first },
                Codec.unboundedMap(Identifier.CODEC, Codec.LONG).optionalFieldOf("progress", emptyMap()).forGetter { it.second },
            ).apply(i, ::Pair)
        }.comapFlatMap(
            { (unlocked, progress) ->
                val set = unlocked.toSet()
                problem(set, progress)?.let { DataResult.error { it } } ?: DataResult.success(Research(set, progress))
            },
            { it.unlocked.sorted() to it.progress.toSortedMap() },
        )

        /** Saves from before progress existed: a plain list of unlocked ids. */
        private val UNLOCKED_ONLY: Codec<Research> = Identifier.CODEC.listOf().xmap({ Research(it.toSet()) }, { it.unlocked.sorted() })

        @JvmField
        val CODEC: Codec<Research> = Codec.withAlternative(RECORD, UNLOCKED_ONLY)

        @JvmStatic
        fun merge(team: Research, joining: Research): Research {
            val unlocked = team.unlocked + joining.unlocked
            val progress = HashMap<Identifier, Long>()
            for ((id, amount) in team.progress.entries + joining.progress.entries) {
                if (id !in unlocked) progress.merge(id, amount, ::maxOf)
            }
            return Research(unlocked, progress)
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
