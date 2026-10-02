package com.itszuvalex.itszulib.api.utility

import net.minecraft.core.BlockPos

data class ChunkCoord(val chunkX: Int, val chunkZ: Int) {
    fun inRangeOf(other: ChunkCoord, chunkRadius: Int): Boolean =
        other.chunkX in (chunkX - chunkRadius)..(chunkX + chunkRadius) &&
            other.chunkZ in (chunkZ - chunkRadius)..(chunkZ + chunkRadius)

    companion object {
        @JvmStatic
        fun of(pos: BlockPos): ChunkCoord = ChunkCoord(pos.x shr 4, pos.z shr 4)

        @JvmStatic
        fun of(loc: Loc4): ChunkCoord = of(loc.pos)
    }
}
