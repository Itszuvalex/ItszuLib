package com.itszuvalex.itszulib.channel

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.store.SafeStore
import com.itszuvalex.itszulib.store.ServerStores
import com.itszuvalex.itszulib.store.StoreFormat
import com.itszuvalex.itszulib.store.StoreManager
import com.mojang.logging.LogUtils
import com.mojang.serialization.DynamicOps
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The server's channels: a [StoreManager] of [ChannelState] (`<world>/data/itszulib/channels.dat`, see [ChannelEvents]).
 * Everything else reads [state] or changes it through [create] and [delete], which know who is in which team.
 */
class ChannelManager : StoreManager<ChannelState>(ChannelState.EMPTY) {
    /**
     * The id of the team [player] is in, or null before they have one.
     */
    fun teamOf(player: UUID): UUID? = ItszuLib.TEAMS.state.teamOf(player)?.id

    /**
     * [player] makes a channel (see [ChannelState.create]); for [ChannelScope.TEAM], in their current team.
     *
     * @return The new channel.
     * @throws ChannelException if refused.
     */
    fun create(player: UUID, scope: ChannelScope, resource: Identifier, name: String): Channel {
        val before = state.channels.keys
        change { it.create(player, teamOf(player), scope, resource, name) }
        return state.channels.values.single { it.id !in before }
    }

    /**
     * [player] deletes channel [id] (see [ChannelState.delete]).
     *
     * @throws ChannelException if refused.
     */
    fun delete(player: UUID, id: UUID) {
        change { it.delete(player, teamOf(player), id) }
    }

    /** The channels of [resource] that [player] may see in [scope]. */
    fun visible(player: UUID, resource: Identifier, scope: ChannelScope): List<Channel> =
        state.visible(player, teamOf(player), resource, scope)

    /** What [binding] reaches for [resource] now, or null: no channel of that name (any more), or its player has left the team. */
    fun resolve(binding: ChannelBinding?, resource: Identifier): Channel? = state.resolve(binding, resource, ::teamOf)
}

/**
 * Persists [ChannelState] through a [SafeStore] (backup fallback, strict decoding, verified atomic writes).
 *
 * Format (version 1): `{ version: 1, channels: [ { id, resource, scope, owner, name } ] }`. Decoding throws on anything
 * unreadable, including channels that break [ChannelState]'s rules.
 */
class ChannelStore(file: Path) : SafeStore<ChannelState>(file, FORMAT, "channel data") {
    companion object {
        const val VERSION = 1

        @JvmField
        val FORMAT: StoreFormat<ChannelState> = object : StoreFormat<ChannelState> {
            override val empty: ChannelState get() = ChannelState.EMPTY

            override fun encode(value: ChannelState, ops: DynamicOps<Tag>): CompoundTag {
                val root = CompoundTag()
                root.putInt("version", VERSION)
                val list = Channel.CODEC.listOf().encodeStart(ops, value.channels.values.sortedBy { it.id }).getOrThrow()
                root.put("channels", list)
                return root
            }

            override fun decode(tag: CompoundTag, ops: DynamicOps<Tag>, source: Path): ChannelState {
                require(tag.getIntOr("version", 0) == VERSION) { "Unknown channel data version in $source" }
                val list = tag.get("channels") ?: throw IllegalArgumentException("No channels in $source")
                val channels = Channel.CODEC.listOf().parse(ops, list).getOrThrow()
                require(channels.map { it.id }.toSet().size == channels.size) { "Two channels share an id in $source" }
                return ChannelState.of(channels.associateBy { it.id })
            }
        }
    }
}

/**
 * The kinds of resource channels exist for, with the names screens show. A channel only needs its resource's id, so a
 * resource nobody registered still works; registering one gives it a name in the channel screen and a place in the
 * list a block offers.
 */
object ChannelResources {
    private val NAMES = ConcurrentHashMap<Identifier, Component>()

    @JvmStatic
    fun register(id: Identifier, name: Component) {
        NAMES[id] = name
    }

    /** The name to show for [id]: the registered one, or its path. */
    @JvmStatic
    fun name(id: Identifier): Component = NAMES[id] ?: Component.literal(id.path)
}

/**
 * Wires [ItszuLib.CHANNELS] into the server: loaded and saved with it as a [ServerStores] store, and team channels of
 * teams that no longer exist removed whenever teams change.
 */
object ChannelEvents {
    private val LOGGER = LogUtils.getLogger()

    @JvmStatic
    fun register() {
        ServerStores.register(Identifier.fromNamespaceAndPath(ItszuLib.ID, "channels.dat"), ItszuLib.CHANNELS, ::ChannelStore)
        ItszuLib.TEAMS.onChange { _, teams -> prune(teams.teams.keys) }
    }

    private fun prune(teams: Set<UUID>) {
        if (!ItszuLib.CHANNELS.isLoaded) return
        try {
            ItszuLib.CHANNELS.change { it.withoutTeamsNotIn(teams) }
        } catch (e: Exception) {
            LOGGER.error("Could not remove the channels of teams that are gone", e)
        }
    }
}
