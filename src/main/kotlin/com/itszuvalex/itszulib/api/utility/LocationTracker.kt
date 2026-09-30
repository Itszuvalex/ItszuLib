package com.itszuvalex.itszulib.api.utility

import com.google.common.math.LongMath
import net.minecraft.resources.Identifier
import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Indexes locations by dimension and chunk, for range and per-chunk queries.
 */
class LocationTracker {
    private val trackerMap = HashMap<Identifier, HashMap<ChunkCoord, HashSet<Loc4>>>()

    fun trackLocation(loc: Loc4) {
        trackerMap.getOrPut(loc.dimensionId, ::HashMap).getOrPut(loc.chunkCoords, ::HashSet).add(loc)
    }

    fun removeLocation(loc: Loc4) {
        val dimMap = trackerMap[loc.dimensionId] ?: return
        val coords = loc.chunkCoords
        val locSet = dimMap[coords] ?: return
        locSet.remove(loc)
        if (locSet.isEmpty()) {
            dimMap.remove(coords)
            if (dimMap.isEmpty()) trackerMap.remove(loc.dimensionId)
        }
    }

    fun isLocationTracked(loc: Loc4): Boolean =
        trackerMap[loc.dimensionId]?.get(loc.chunkCoords)?.contains(loc) ?: false

    fun getTrackedLocationsInDim(dim: Identifier): Sequence<Loc4> =
        trackerMap[dim]?.values?.asSequence()?.flatten() ?: emptySequence()

    fun getTrackedLocationsInChunk(dim: Identifier, coords: ChunkCoord): Sequence<Loc4> =
        trackerMap[dim]?.get(coords)?.asSequence() ?: emptySequence()

    fun getAllTrackedLocations(): Sequence<Loc4> = trackerMap.values.asSequence().flatMap { it.values }.flatten()

    fun clear() = trackerMap.clear()

    fun clearDim(dim: Identifier) {
        trackerMap[dim]?.clear()
    }

    fun clearChunk(dim: Identifier, coord: ChunkCoord) {
        trackerMap[dim]?.get(coord)?.clear()
    }

    fun getLocationsInRange(loc: Loc4, range: Float): Sequence<Loc4> {
        val rangesqr = range.toDouble() * range
        return chunksToCheck(loc.dimensionId, loc.chunkCoords, range)
            .flatMap { getLocationsInChunk(loc.dimensionId, it) }
            .filter { it.distSqr(loc) <= rangesqr }
    }

    fun getLocationsInRange(dim: Identifier, loc: Vector3f, range: Float): Sequence<Loc4> {
        // Floor, not truncate: truncating -0.5 gives 0, but the block is in chunk -1.
        val chunkCoords = ChunkCoord(floor(loc.x()).toInt() shr 4, floor(loc.z()).toInt() shr 4)
        val rangesqr = range.toDouble() * range
        return chunksToCheck(dim, chunkCoords, range)
            .flatMap { getLocationsInChunk(dim, it) }
            .filter {
                val dx = it.x - loc.x().toDouble()
                val dy = it.y - loc.y().toDouble()
                val dz = it.z - loc.z().toDouble()
                dx * dx + dy * dy + dz * dz <= rangesqr
            }
    }

    /**
     * If (radius in chunks)^2 is smaller than the number of chunks tracked in the dimension, generate the candidate
     * chunk coordinates and look each up; otherwise scan the tracked chunks and filter by chunk distance.
     */
    private fun chunksToCheck(dim: Identifier, center: ChunkCoord, range: Float): Sequence<ChunkCoord> {
        val radius = ceil(range / MCConstants.CHUNK_SIZE).toInt()
        val rsqr = try {
            LongMath.checkedMultiply(radius.toLong(), radius.toLong())
        } catch (e: ArithmeticException) {
            -1L
        }
        val tracked = trackerMap[dim]?.size ?: 0
        return if (rsqr in 1..<tracked) getChunkCoordsInRadius(center, radius)
        else getChunkCoordsInRadiusInDim(center, radius, dim)
    }

    internal fun getLocationsInChunk(dim: Identifier, chunkLoc: ChunkCoord): Sequence<Loc4> =
        trackerMap[dim]?.get(chunkLoc)?.asSequence() ?: emptySequence()

    private fun getChunkCoordsInRadius(coords: ChunkCoord, radius: Int): Sequence<ChunkCoord> =
        (-radius..radius).asSequence().flatMap { i ->
            (-radius..radius).asSequence().map { j -> ChunkCoord(coords.chunkX + i, coords.chunkZ + j) }
        }

    private fun getChunkCoordsInRadiusInDim(loc: ChunkCoord, radius: Int, dim: Identifier): Sequence<ChunkCoord> =
        trackerMap[dim]?.keys?.asSequence()?.filter { it.inRangeOf(loc, radius) } ?: emptySequence()
}
