package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.Components
import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.utility.ChunkCoord
import com.itszuvalex.itszulib.api.utility.LazySingleSidedHolder
import com.itszuvalex.itszulib.core.NetworkManager
import com.itszuvalex.itszulib.dev.DevContent
import com.itszuvalex.itszulib.network.ItszuLibNetwork
import com.itszuvalex.itszulib.team.Research
import com.itszuvalex.itszulib.team.TeamDataTypes
import com.itszuvalex.itszulib.team.TeamEvents
import com.itszuvalex.itszulib.team.TeamManager
import com.mojang.logging.LogUtils
import net.minecraft.world.level.Level
import net.neoforged.fml.LogicalSide
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.level.ChunkEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import org.slf4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

/**
 * ItszuLib mod entry point. Loaded by Kotlin for Forge (`modLoader="kotlinforforge"`), which instantiates this object.
 */
@Mod(ItszuLib.ID)
object ItszuLib {
    const val ID = "itszulib"
    const val NAME = "ItszuLib"

    @JvmField
    val LOGGER: Logger = LogUtils.getLogger()

    /**
     * Server-side registry of live block entity networks, ticked from server tick events.
     */
    @JvmField
    val NETWORK_MANAGER = LazySingleSidedHolder(::NetworkManager, LogicalSide.SERVER)

    /**
     * Teams and their per-team data, loaded when the server starts and saved with the overworld. See [TeamManager].
     */
    @JvmField
    val TEAMS = TeamManager()

    init {
        // Built-in modules must exist before RegisterCapabilitiesEvent
        Modules.init()
        Components.register(MOD_BUS)
        MOD_BUS.addListener(ItszuLibNetwork::register)
        TeamDataTypes.register(Research.TYPE)
        TeamEvents.register()

        if (!FMLEnvironment.isProduction()) {
            DevContent.register(MOD_BUS)
        }

        NeoForge.EVENT_BUS.addListener { _: ServerStoppedEvent -> NETWORK_MANAGER.get(LogicalSide.SERVER)?.clear() }
        NeoForge.EVENT_BUS.addListener { _: ServerTickEvent.Pre -> NETWORK_MANAGER.get(LogicalSide.SERVER)?.onTickStart() }
        NeoForge.EVENT_BUS.addListener { _: ServerTickEvent.Post -> NETWORK_MANAGER.get(LogicalSide.SERVER)?.onTickEnd() }
        NeoForge.EVENT_BUS.addListener(::onChunkUnload)
    }

    /**
     * Networks only track loaded block entities; drop nodes in a chunk as a batch when it unloads.
     */
    private fun onChunkUnload(event: ChunkEvent.Unload) {
        val level = event.level as? Level ?: return
        if (level.isClientSide) return
        val pos = event.chunk.pos
        NETWORK_MANAGER.get(LogicalSide.SERVER)?.onChunkUnload(ILevel.of(level), ChunkCoord(pos.x(), pos.z()))
    }
}
