package com.itszuvalex.itszulib.channel

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.Identifier
import java.util.UUID

/**
 * Whose a channel is: one player's ([PLAYER]), or their team's ([TEAM]: any member may use, create and delete it).
 */
enum class ChannelScope { PLAYER, TEAM }

/**
 * A named channel for one kind of resource (power, items, fluid, nanites, anything a mod names with an id), which
 * blocks join to send and receive that resource between them. Blocks join by name (see [ChannelBinding]), so a deleted
 * channel that is made again under the same name takes its blocks back; [id] tells the two apart where that matters.
 *
 * @param owner The player ([PLAYER]) or the team ([TEAM]) that has it.
 */
data class Channel(val id: UUID, val resource: Identifier, val scope: ChannelScope, val owner: UUID, val name: String) {
    companion object {
        @JvmField
        val CODEC: Codec<Channel> = RecordCodecBuilder.create { i ->
            i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Channel::id),
                Identifier.CODEC.fieldOf("resource").forGetter(Channel::resource),
                Codec.STRING.xmap({ n -> ChannelScope.entries.firstOrNull { it.name == n } ?: throw IllegalArgumentException("Unknown channel scope $n") }, ChannelScope::name)
                    .fieldOf("scope").forGetter(Channel::scope),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Channel::owner),
                Codec.STRING.fieldOf("name").forGetter(Channel::name),
            ).apply(i, ::Channel)
        }
    }
}

/**
 * A request about channels that was refused (a bad or taken name, a channel that is not there, a player who may not).
 * Nothing was changed. [message] can be shown to the player.
 */
class ChannelException(message: String) : RuntimeException(message)

/**
 * Where a block joins a channel of its resource: the player who chose it ([owner]), whose channels ([scope]) and the
 * channel's [name]. It is by name, so a channel that was deleted and is made again under the same name takes its blocks
 * back (an accidental delete can be undone, and a naming pattern can join blocks as they load), and a binding is kept
 * while no channel matches, so the block has none meanwhile (see [ChannelState.resolve]). For a [ChannelScope.TEAM]
 * channel it means the channel of that name in the team [owner] is in now, so a player who has left the team reaches
 * none of its channels.
 */
data class ChannelBinding(val owner: UUID, val scope: ChannelScope, val name: String) {
    companion object {
        @JvmField
        val CODEC: Codec<ChannelBinding> = RecordCodecBuilder.create { i ->
            i.group(
                UUIDUtil.CODEC.fieldOf("owner").forGetter(ChannelBinding::owner),
                Codec.STRING.xmap({ n -> ChannelScope.entries.firstOrNull { it.name == n } ?: throw IllegalArgumentException("Unknown channel scope $n") }, ChannelScope::name)
                    .fieldOf("scope").forGetter(ChannelBinding::scope),
                Codec.STRING.fieldOf("name").forGetter(ChannelBinding::name),
            ).apply(i, ::ChannelBinding)
        }

        /** The binding of [player] to [channel]. */
        @JvmStatic
        fun to(player: UUID, channel: Channel) = ChannelBinding(player, channel.scope, channel.name)
    }
}

/**
 * Every channel, immutable: each change returns a new state, built through [of] which checks that names are valid and
 * unique for their owner and resource. Pure, so the rules are tested without a game; the server's copy is
 * [com.itszuvalex.itszulib.ItszuLib.CHANNELS].
 *
 * Teams are not known here: operations that depend on who is in which team take the player's team id.
 */
class ChannelState private constructor(val channels: Map<UUID, Channel>) {
    operator fun get(id: UUID): Channel? = channels[id]

    /**
     * The channels of [resource] that [player] may see in [scope]: their own, or their team's, by name.
     */
    fun visible(player: UUID, team: UUID?, resource: Identifier, scope: ChannelScope): List<Channel> {
        val owner = if (scope == ChannelScope.PLAYER) player else team ?: return emptyList()
        return channels.values.filter { it.resource == resource && it.scope == scope && it.owner == owner }.sortedBy { it.name.lowercase() }
    }

    /**
     * Whether [player] may change [channel]: a player's own channels, or any channel of the team they are in.
     */
    fun mayManage(channel: Channel, player: UUID, team: UUID?): Boolean = when (channel.scope) {
        ChannelScope.PLAYER -> channel.owner == player
        ChannelScope.TEAM -> team != null && channel.owner == team
    }

    /**
     * [player] makes a channel named [name] for [resource] in [scope] (for TEAM, their [team]'s).
     *
     * @throws ChannelException for a bad name, a name already used for that resource by that owner, or a team channel
     * with no team.
     */
    fun create(player: UUID, team: UUID?, scope: ChannelScope, resource: Identifier, name: String, newId: () -> UUID = UUID::randomUUID): ChannelState {
        val clean = clean(name)
        val owner = if (scope == ChannelScope.PLAYER) player else team ?: throw ChannelException("You are not in a team.")
        if (channels.values.any { it.resource == resource && it.scope == scope && it.owner == owner && it.name.equals(clean, ignoreCase = true) }) {
            throw ChannelException("There is already a channel called \"$clean\".")
        }
        val channel = Channel(newId(), resource, scope, owner, clean)
        return of(channels + (channel.id to channel))
    }

    /**
     * [player] deletes channel [id].
     *
     * @throws ChannelException if there is no such channel, or [player] may not change it.
     */
    fun delete(player: UUID, team: UUID?, id: UUID): ChannelState {
        val channel = channels[id] ?: throw ChannelException("That channel no longer exists.")
        if (!mayManage(channel, player, team)) throw ChannelException("You cannot delete that channel.")
        return of(channels - id)
    }

    /**
     * What [binding] reaches for [resource]: the channel of its name, in the bound player's own channels or their
     * current team's, if there is one; null otherwise, so the block has no channel.
     */
    fun resolve(binding: ChannelBinding?, resource: Identifier, teamOf: (UUID) -> UUID?): Channel? {
        binding ?: return null
        val owner = if (binding.scope == ChannelScope.PLAYER) binding.owner else teamOf(binding.owner) ?: return null
        return channels.values.firstOrNull {
            it.resource == resource && it.scope == binding.scope && it.owner == owner && it.name.equals(binding.name, ignoreCase = true)
        }
    }

    /**
     * The state without the team channels of teams that are gone (a disbanded team's): [teams] are the teams that exist.
     */
    fun withoutTeamsNotIn(teams: Set<UUID>): ChannelState {
        val kept = channels.filterValues { it.scope == ChannelScope.PLAYER || it.owner in teams }
        return if (kept.size == channels.size) this else of(kept)
    }

    override fun equals(other: Any?): Boolean = other is ChannelState && other.channels == channels

    override fun hashCode(): Int = channels.hashCode()

    companion object {
        const val MAX_NAME = 32

        @JvmField
        val EMPTY = ChannelState(emptyMap())

        /** The name as stored: trimmed, and not blank, longer than [MAX_NAME] or holding control characters. */
        @JvmStatic
        fun clean(name: String): String {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) throw ChannelException("A channel needs a name.")
            if (trimmed.length > MAX_NAME) throw ChannelException("A channel name can be at most $MAX_NAME characters.")
            if (trimmed.any { it.isISOControl() }) throw ChannelException("A channel name cannot hold control characters.")
            return trimmed
        }

        /**
         * @throws IllegalArgumentException if a channel's name is not valid, or two channels of the same owner, scope
         * and resource share a name.
         */
        @JvmStatic
        fun of(channels: Map<UUID, Channel>): ChannelState {
            val seen = HashSet<List<Any>>()
            for ((id, c) in channels) {
                require(c.id == id) { "Channel $id is stored under another id" }
                require(runCatching { clean(c.name) }.getOrNull() == c.name) { "Channel $id has an invalid name \"${c.name}\"" }
                require(seen.add(listOf(c.resource, c.scope, c.owner, c.name.lowercase()))) { "Two channels called \"${c.name}\"" }
            }
            return ChannelState(channels.toMap())
        }
    }
}
