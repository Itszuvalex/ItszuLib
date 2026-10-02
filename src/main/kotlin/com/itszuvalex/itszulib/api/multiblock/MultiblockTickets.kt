package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.utility.ChunkCoord
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.TicketType
import net.minecraft.world.level.ChunkPos
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * The `itszulib:multiblock` chunk ticket: loads the home chunk of a structure with a ticking member elsewhere (see
 * [MultiblockManager]). Loads without ticking, lapses after [MultiblockManager.TICKET_TIMEOUT] ticks unless refreshed,
 * and is not saved.
 */
object VanillaChunkTickets : IChunkTickets {
    private val TICKET_TYPES: DeferredRegister<TicketType> = DeferredRegister.create(Registries.TICKET_TYPE, ItszuLib.ID)

    @JvmField
    val MULTIBLOCK: DeferredHolder<TicketType, TicketType> =
        TICKET_TYPES.register("multiblock") { -> TicketType(MultiblockManager.TICKET_TIMEOUT, TicketType.FLAG_LOADING) }

    fun register(bus: IEventBus) = TICKET_TYPES.register(bus)

    override fun refresh(level: ILevel, chunk: ChunkCoord) {
        (level.toMinecraft() as? ServerLevel)?.chunkSource?.addTicketWithRadius(MULTIBLOCK.get(), ChunkPos(chunk.chunkX, chunk.chunkZ), 0)
    }

    override fun isTicking(level: ILevel, pos: BlockPos): Boolean =
        (level.toMinecraft() as? ServerLevel)?.shouldTickBlocksAt(ChunkPos.pack(pos)) ?: false
}
