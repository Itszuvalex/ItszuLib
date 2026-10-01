package com.itszuvalex.itszulib.team

import com.itszuvalex.itszulib.ItszuLib
import com.mojang.logging.LogUtils
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.LevelResource
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.server.ServerLifecycleHooks

/**
 * Server to client: the receiving player's own team, encoded as [TeamCodec] saves it.
 */
data class TeamSyncPayload(val team: CompoundTag) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<TeamSyncPayload> = TYPE

    companion object {
        @JvmField
        val TYPE: CustomPacketPayload.Type<TeamSyncPayload> =
            CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(ItszuLib.ID, "team_sync"))

        @JvmField
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, TeamSyncPayload> =
            ByteBufCodecs.COMPOUND_TAG.map(::TeamSyncPayload, TeamSyncPayload::team).cast()

        /**
         * Client side. A payload that does not decode is logged and ignored; the previous copy stays.
         */
        @JvmStatic
        fun handle(payload: TeamSyncPayload, context: IPayloadContext) {
            try {
                ClientTeam.current = TeamCodec.decodeTeam(payload.team, context.player().registryAccess().createSerializationContext(NbtOps.INSTANCE))
            } catch (e: Exception) {
                LOGGER.error("Ignoring a team sync that could not be read", e)
            }
        }

        private val LOGGER = LogUtils.getLogger()
    }
}

/**
 * The client's read-only copy of its own team, replaced whenever the server sends a [TeamSyncPayload] (on login and
 * after every change to the team). Null before the first sync.
 */
object ClientTeam {
    @Volatile
    var current: Team? = null
        internal set
}

/**
 * Wires [ItszuLib.TEAMS] into the server lifecycle: load when the server starts, save with the overworld (autosave
 * and shutdown), give each joining player a solo team, sync each player's team, and register `/itszulib team`. The
 * sync payload is registered with ItszuLib's others in [com.itszuvalex.itszulib.network.ItszuLibNetwork].
 */
object TeamEvents {
    @JvmStatic
    fun register() {
        NeoForge.EVENT_BUS.addListener(::onServerStarting)
        NeoForge.EVENT_BUS.addListener(::onLevelSave)
        NeoForge.EVENT_BUS.addListener(::onServerStopped)
        NeoForge.EVENT_BUS.addListener(::onLogin)
        NeoForge.EVENT_BUS.addListener { event: RegisterCommandsEvent -> TeamCommands.register(event.dispatcher) }
        ItszuLib.TEAMS.onChange(::syncChanged)
    }

    private fun onServerStarting(event: ServerStartingEvent) {
        val server = event.server
        val file = server.getWorldPath(LevelResource.DATA).resolve(ItszuLib.ID).resolve("teams.dat")
        ItszuLib.TEAMS.load(TeamStore(file), server.registryAccess().createSerializationContext(NbtOps.INSTANCE))
    }

    private fun onLevelSave(event: LevelEvent.Save) {
        val level = event.level as? ServerLevel ?: return
        if (level.dimension() == Level.OVERWORLD) ItszuLib.TEAMS.save()
    }

    private fun onServerStopped(event: ServerStoppedEvent) {
        ItszuLib.TEAMS.save()
        ItszuLib.TEAMS.unload()
    }

    private fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        ItszuLib.TEAMS.change { it.ensurePlayer(player.uuid, player.name.string) }
        send(player)
    }

    private fun syncChanged(old: TeamState, new: TeamState) {
        val server = ServerLifecycleHooks.getCurrentServer() ?: return
        for (player in server.playerList.players) {
            if (old.teamOf(player.uuid) != new.teamOf(player.uuid)) send(player)
        }
    }

    /**
     * Only to connections that negotiated the payload: a client without the mod, or a game test's mock player, cannot
     * receive it, and sending would throw.
     */
    private fun send(player: ServerPlayer) {
        if (!player.connection.hasChannel(TeamSyncPayload.TYPE)) return
        val team = ItszuLib.TEAMS.state.teamOf(player.uuid) ?: return
        val ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE)
        PacketDistributor.sendToPlayer(player, TeamSyncPayload(TeamCodec.encodeTeam(team, ops)))
    }
}
