package com.itszuvalex.itszulib.team

import com.itszuvalex.itszulib.ItszuLib
import com.mojang.serialization.Codec
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
 * The research a team has unlocked. Only grows: joining a team unions research, leaving copies it.
 */
data class Research(val unlocked: Set<Identifier>) {
    fun has(id: Identifier): Boolean = id in unlocked

    fun unlock(id: Identifier): Research = if (has(id)) this else Research(unlocked + id)

    companion object {
        @JvmField
        val EMPTY = Research(emptySet())

        @JvmField
        val CODEC: Codec<Research> = Identifier.CODEC.listOf().xmap({ Research(it.toSet()) }, { it.unlocked.sorted() })

        @JvmField
        val TYPE: TeamDataType<Research> = TeamDataType(
            Identifier.fromNamespaceAndPath(ItszuLib.ID, "research"),
            CODEC,
            { EMPTY },
            { team, joining -> Research(team.unlocked + joining.unlocked) },
        )
    }
}
