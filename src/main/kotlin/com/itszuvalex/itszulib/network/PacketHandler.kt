package com.itszuvalex.itszulib.network

import com.itszuvalex.itszulib.menu.MenuActionPayload
import com.itszuvalex.itszulib.menu.MenuTextActionPayload
import com.itszuvalex.itszulib.menu.MenuSyncPayload
import com.itszuvalex.itszulib.team.TeamSyncPayload
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.registration.PayloadRegistrar

/**
 * Registers network payloads. Construct from a [RegisterPayloadHandlersEvent] listener on the mod bus, then register
 * payloads with [registrar]. Port of ItszuLib 1.12.2's `PacketHandler` (a `SimpleNetworkWrapper` channel); payloads are
 * identified by their `CustomPacketPayload.Type` instead of a per-channel byte id.
 */
class PacketHandler(event: RegisterPayloadHandlersEvent, version: String) {
    @JvmField
    val registrar: PayloadRegistrar = event.registrar(version)
}

/**
 * ItszuLib's own payloads: menu value syncs and team syncs (server to client), and menu actions (client to server).
 */
object ItszuLibNetwork {
    /**
     * Bump when a payload's wire format changes.
     */
    const val VERSION = "2"

    @JvmStatic
    fun register(event: RegisterPayloadHandlersEvent) {
        val handler = PacketHandler(event, VERSION)
        handler.registrar.playToClient(MenuSyncPayload.TYPE, MenuSyncPayload.STREAM_CODEC, MenuSyncPayload::handle)
        handler.registrar.playToServer(MenuActionPayload.TYPE, MenuActionPayload.STREAM_CODEC, MenuActionPayload::handle)
        handler.registrar.playToServer(MenuTextActionPayload.TYPE, MenuTextActionPayload.STREAM_CODEC, MenuTextActionPayload::handle)
        handler.registrar.playToClient(TeamSyncPayload.TYPE, TeamSyncPayload.STREAM_CODEC, TeamSyncPayload::handle)
    }
}
