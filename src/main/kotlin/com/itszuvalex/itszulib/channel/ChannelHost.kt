package com.itszuvalex.itszulib.channel

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.core.frag.BlockEntityFragment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID

/**
 * A block that can join channels, one per kind of resource it offers ([resources]): the module behind a block's
 * channel menu ([Modules.CHANNELS]) and what the block's own code asks for the channel to send or receive on.
 */
interface IChannelHost {
    /** The kinds of resource this block can join a channel for, in the order a screen lists them. */
    val resources: List<Identifier>

    /**
     * What [resource] is joined to, by name: kept while no channel of that name exists (deleted, or its player has left the
     * team that had it), and joins again if one is made. [channel] says what it reaches now.
     */
    fun binding(resource: Identifier): ChannelBinding?

    /**
     * The channel this block sends and receives [resource] on now, or null: it has none, or none of its name exists (it
     * was deleted, or the player who chose it has left the team that had it). A block with no channel moves nothing.
     */
    fun channel(resource: Identifier): Channel?

    /**
     * [player] joins this block to [channel] for its resource, or leaves it with none when [channel] is null.
     *
     * @throws ChannelException if [player] may not use that channel, or this block has no use for its resource.
     */
    fun bind(resource: Identifier, player: UUID, channel: Channel?)
}

/**
 * Which loaded blocks are joined to which channels, so a screen can say how many are on a channel. Server side; blocks
 * add themselves when they load and leave when they unload or are removed ([FragChannel]).
 */
object ChannelUsers {
    private val hosts: MutableSet<FragChannel> = Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap()))

    @JvmStatic
    fun add(host: FragChannel) {
        hosts += host
    }

    @JvmStatic
    fun remove(host: FragChannel) {
        hosts -= host
    }

    /** How many loaded blocks are on [channel] (each resource of a block counts once). */
    @JvmStatic
    fun count(channel: Channel): Int = synchronized(hosts) { hosts.sumOf { it.countOn(channel) } }

    /** Forgets every block (for tests and server shutdown). */
    @JvmStatic
    fun clear() = hosts.clear()
}

/**
 * Gives a block entity channels, one binding per resource in [resources], saved with it. Bindings are by name and kept:
 * while no channel of the name exists (deleted, or its player left the team that had it) the block has no channel
 * ([channel] is null) and moves nothing, and it joins again when one is made, whether the block is loaded or loads later.
 */
class FragChannel(override val resources: List<Identifier>) : BlockEntityFragment<IChannelHost>(), IChannelHost {
    private val bindings = LinkedHashMap<Identifier, ChannelBinding>()

    init {
        require(resources.isNotEmpty()) { "A block with channels needs at least one resource" }
        require(resources.toSet().size == resources.size) { "Duplicate channel resources" }
    }

    override fun name(): String = NAME

    override fun module(): IModule<IChannelHost> = Modules.CHANNELS

    override fun faceToModuleMapper(be: IBlockEntity): (Direction?) -> IChannelHost? = { this }

    override fun binding(resource: Identifier): ChannelBinding? = bindings[resource]

    override fun channel(resource: Identifier): Channel? {
        val binding = bindings[resource] ?: return null
        return if (ItszuLib.CHANNELS.isLoaded) ItszuLib.CHANNELS.resolve(binding, resource) else null
    }

    override fun bind(resource: Identifier, player: UUID, channel: Channel?) {
        if (resource !in resources) throw ChannelException("This block has no use for that kind of channel.")
        if (channel == null) {
            if (bindings.remove(resource) != null) markDirty()
            return
        }
        if (channel.resource != resource) throw ChannelException("That channel is for something else.")
        if (!ItszuLib.CHANNELS.state.mayManage(channel, player, ItszuLib.CHANNELS.teamOf(player))) throw ChannelException("You cannot use that channel.")
        bindings[resource] = ChannelBinding.to(player, channel)
        markDirty()
    }

    internal fun countOn(channel: Channel): Int = bindings.entries.count { (resource, binding) ->
        resource == channel.resource && ItszuLib.CHANNELS.resolve(binding, resource)?.id == channel.id
    }

    override fun handlesScope(scope: NBTSerializationScope): Boolean = scope == NBTSerializationScope.LEVEL

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) {
        for ((resource, binding) in bindings) output.store(resource.toString(), ChannelBinding.CODEC, binding)
    }

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) {
        bindings.clear()
        for (resource in resources) input.read(resource.toString(), ChannelBinding.CODEC).ifPresent { bindings[resource] = it }
    }

    override fun onLoad(level: ILevel, pos: BlockPos) {
        if (level.isClientSide()) return
        ChannelUsers.add(this)
    }

    override fun onChunkUnloaded(level: ILevel, pos: BlockPos) {
        ChannelUsers.remove(this)
    }

    override fun invalidateFrags() {
        ChannelUsers.remove(this)
    }

    override fun rehydrateFrags() {
        if (onServer()) ChannelUsers.add(this)
    }

    /** Whether this block entity is in a server level (set before it is added, so also true for a block just placed). */
    private fun onServer(): Boolean = host?.blockEntity()?.toMinecraft()?.level?.isClientSide == false

    override fun onRemove(level: ILevel, pos: BlockPos, blockStatePrev: BlockState) {
        ChannelUsers.remove(this)
    }

    companion object {
        const val NAME = "Channels"
    }
}
