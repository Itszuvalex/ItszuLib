package com.itszuvalex.itszulib.menu

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.channel.Channel
import com.itszuvalex.itszulib.channel.ChannelException
import com.itszuvalex.itszulib.channel.ChannelScope
import com.itszuvalex.itszulib.channel.ChannelState
import com.itszuvalex.itszulib.channel.ChannelUsers
import com.itszuvalex.itszulib.channel.IChannelHost
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Player
import java.util.UUID

/**
 * A channel in the list a player sees: [count] is how many loaded blocks are on it.
 */
data class ChannelEntry(val id: UUID, val name: String, val count: Int)

/**
 * What a channel screen shows, kept in step with the server ([MenuChannels.sync]): the block's resources and the one
 * being looked at, the scope being looked at and the channels the player can see in it, what the block is on now
 * (which may be in the other scope), and the last refusal.
 *
 * @param currentName The channel the block is joined to for this resource, or empty; [currentScope] says whose it is.
 * @param currentId That channel's id, or null if the block has none now: [currentName] is empty, or no channel of that
 * name exists ([currentMissing]; it joins again if one is made).
 * @param message Why the last request was refused, or empty.
 */
data class ChannelView(
    val resources: List<Identifier>,
    val resourceIndex: Int,
    val scope: ChannelScope,
    val inTeam: Boolean,
    val channels: List<ChannelEntry>,
    val currentId: UUID?,
    val currentName: String,
    val currentScope: ChannelScope,
    val currentMissing: Boolean,
    val message: String,
) {
    val resource: Identifier? get() = resources.getOrNull(resourceIndex)

    companion object {
        @JvmField
        val EMPTY = ChannelView(emptyList(), 0, ChannelScope.PLAYER, false, emptyList(), null, "", ChannelScope.PLAYER, false, "")

        private val ENTRY: StreamCodec<RegistryFriendlyByteBuf, ChannelEntry> = StreamCodec.of(
            { buf, e ->
                buf.writeUUID(e.id)
                buf.writeUtf(e.name, ChannelState.MAX_NAME)
                buf.writeVarInt(e.count)
            },
            { buf -> ChannelEntry(buf.readUUID(), buf.readUtf(ChannelState.MAX_NAME), buf.readVarInt()) },
        )

        @JvmField
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ChannelView> = StreamCodec.of(
            { buf, v ->
                buf.writeVarInt(v.resources.size)
                v.resources.forEach(buf::writeIdentifier)
                buf.writeVarInt(v.resourceIndex)
                buf.writeEnum(v.scope)
                buf.writeBoolean(v.inTeam)
                buf.writeVarInt(v.channels.size)
                v.channels.forEach { ENTRY.encode(buf, it) }
                buf.writeBoolean(v.currentId != null)
                v.currentId?.let(buf::writeUUID)
                buf.writeUtf(v.currentName, ChannelState.MAX_NAME)
                buf.writeEnum(v.currentScope)
                buf.writeBoolean(v.currentMissing)
                buf.writeUtf(v.message, 256)
            },
            { buf ->
                val resources = List(buf.readVarInt()) { buf.readIdentifier() }
                val index = buf.readVarInt()
                val scope = buf.readEnum(ChannelScope::class.java)
                val inTeam = buf.readBoolean()
                val channels = List(buf.readVarInt()) { ENTRY.decode(buf) }
                val currentId = if (buf.readBoolean()) buf.readUUID() else null
                ChannelView(resources, index, scope, inTeam, channels, currentId, buf.readUtf(ChannelState.MAX_NAME), buf.readEnum(ChannelScope::class.java), buf.readBoolean(), buf.readUtf(256))
            },
        )
    }
}

/**
 * Lets a menu's screen join its block to channels ([MenuCore.enableChannels]): pick a resource (if the block offers
 * several), look at the player's own channels or their team's, join one, leave, make one by name, or delete one. The
 * server keeps what is being looked at and checks every request; the screen only draws [view] and sends requests.
 *
 * Actions: [ACTION_RESOURCE] and [ACTION_SCOPE] take their value in the action's int; [ACTION_SELECT], [ACTION_DELETE] and
 * [ACTION_CREATE] take text (a channel id, empty to leave, or a new name) through [MenuTextActionPayload].
 */
class MenuChannels(private val player: Player?, private val host: () -> IChannelHost?) {
    /** The client's copy, set by [sync]. */
    @JvmField
    var view: ChannelView = ChannelView.EMPTY

    private var resourceIndex = 0
    private var scope = ChannelScope.PLAYER
    private var message = ""

    @JvmField
    val sync: MenuSync<ChannelView> = MenuSync({ compute() }, { view = it }, ChannelView.CODEC)

    /** Server side: what the screen should show now. */
    fun compute(): ChannelView {
        val host = host() ?: return ChannelView.EMPTY
        val uuid = player?.uuid ?: return ChannelView.EMPTY
        val resources = host.resources
        val index = resourceIndex.coerceIn(0, resources.lastIndex)
        val resource = resources[index]
        val manager = ItszuLib.CHANNELS
        val team = manager.teamOf(uuid)
        val visible = manager.state.visible(uuid, team, resource, scope).map { ChannelEntry(it.id, it.name, ChannelUsers.count(it)) }
        val current = host.channel(resource)
        val binding = host.binding(resource)
        return ChannelView(
            resources, index, scope, team != null, visible, current?.id, current?.name ?: binding?.name ?: "",
            current?.scope ?: binding?.scope ?: ChannelScope.PLAYER, current == null && binding != null, message,
        )
    }

    /** Server side: [ACTION_RESOURCE] or [ACTION_SCOPE]. */
    fun handle(action: Int, data: Int): Boolean {
        val host = host() ?: return false
        message = ""
        when (action) {
            ACTION_RESOURCE -> resourceIndex = data.coerceIn(0, host.resources.lastIndex)
            ACTION_SCOPE -> scope = ChannelScope.entries.getOrNull(data) ?: return false
            else -> return false
        }
        return true
    }

    /** Server side: [ACTION_SELECT], [ACTION_DELETE] or [ACTION_CREATE]. Refusals become the view's message. */
    fun handleText(action: Int, text: String): Boolean {
        val host = host() ?: return false
        val uuid = player?.uuid ?: return false
        val resource = host.resources[resourceIndex.coerceIn(0, host.resources.lastIndex)]
        message = ""
        try {
            when (action) {
                ACTION_SELECT -> host.bind(resource, uuid, if (text.isEmpty()) null else find(text, resource))
                ACTION_DELETE -> ItszuLib.CHANNELS.delete(uuid, find(text, resource).id)
                ACTION_CREATE -> host.bind(resource, uuid, ItszuLib.CHANNELS.create(uuid, scope, resource, text))
                else -> return false
            }
        } catch (e: ChannelException) {
            message = e.message ?: "Refused"
        }
        return true
    }

    private fun find(text: String, resource: Identifier): Channel {
        val id = runCatching { UUID.fromString(text) }.getOrNull() ?: throw ChannelException("That is not a channel.")
        val channel = ItszuLib.CHANNELS.state[id] ?: throw ChannelException("That channel no longer exists.")
        if (channel.resource != resource) throw ChannelException("That channel is for something else.")
        return channel
    }

    companion object {
        const val ACTION_RESOURCE = -2
        const val ACTION_SCOPE = -3
        const val ACTION_SELECT = -4
        const val ACTION_DELETE = -5
        const val ACTION_CREATE = -6

        @JvmField
        val ACTIONS = setOf(ACTION_RESOURCE, ACTION_SCOPE)

        @JvmField
        val TEXT_ACTIONS = setOf(ACTION_SELECT, ACTION_DELETE, ACTION_CREATE)
    }
}
