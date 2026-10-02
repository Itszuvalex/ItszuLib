package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import java.util.Objects
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A block position in a dimension. Subclasses differ only in how they reach the world: [Loc4Level] holds a [Level],
 * [Loc4ILevel] an [ILevel], and [Loc4Indirect] only the dimension id (it cannot look anything up).
 *
 * Equality and ordering are by value (dimension + position) regardless of subclass, so any two [Loc4]s for the
 * same block are interchangeable as map keys.
 */
abstract class Loc4(val pos: BlockPos) : Comparable<Loc4> {
    abstract val dimensionId: Identifier

    abstract fun getOffset(x: Int, y: Int, z: Int): Loc4

    abstract fun copy(): Loc4

    val x: Int get() = pos.x
    val y: Int get() = pos.y
    val z: Int get() = pos.z

    fun getOffset(offset: BlockPos): Loc4 = getOffset(offset.x, offset.y, offset.z)

    val chunkCoords: ChunkCoord get() = ChunkCoord.of(pos)

    fun anchor(level: Level): Loc4 = Loc4Level(level, pos)

    fun anchor(level: ILevel): Loc4 = Loc4ILevel(level, pos)

    /**
     * @param force Look up even if the chunk is not loaded (may load it).
     */
    abstract fun getIBlockEntity(force: Boolean): IBlockEntity?

    abstract fun getBlockEntity(force: Boolean): BlockEntity?

    fun distSqr(other: Loc4): Double {
        if (other.dimensionId != dimensionId) return Double.MAX_VALUE
        return distSqr(other.x, other.y, other.z)
    }

    fun distSqr(x: Int, y: Int, z: Int): Double {
        val dx = (this.x - x).toDouble()
        val dy = (this.y - y).toDouble()
        val dz = (this.z - z).toDouble()
        return dx * dx + dy * dy + dz * dz
    }

    fun dist(other: Loc4): Double {
        if (other.dimensionId != dimensionId) return Float.MAX_VALUE.toDouble()
        return dist(other.x, other.y, other.z)
    }

    fun dist(x: Int, y: Int, z: Int): Double = sqrt(distSqr(x, y, z))

    override fun compareTo(other: Loc4): Int {
        val dim = dimensionId.compareTo(other.dimensionId)
        if (dim != 0) return dim
        val xl = x.compareTo(other.x)
        if (xl != 0) return xl
        val yl = y.compareTo(other.y)
        if (yl != 0) return yl
        return z.compareTo(other.z)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Loc4) return false
        return dimensionId == other.dimensionId && pos == other.pos
    }

    override fun hashCode(): Int = Objects.hash(dimensionId, pos)

    override fun toString(): String = "Loc4[$dimensionId ${pos.x},${pos.y},${pos.z}]"

    fun isNeighbor(loc: Loc4): Boolean {
        if (dimensionId != loc.dimensionId) return false
        if (abs(x - loc.x) == 1 && y == loc.y && z == loc.z) return true
        if (x == loc.x && abs(y - loc.y) == 1 && z == loc.z) return true
        return x == loc.x && y == loc.y && abs(z - loc.z) == 1
    }

    companion object {
        @JvmStatic
        fun of(worldId: Identifier, loc: BlockPos): Loc4 = Loc4Indirect(worldId, loc)

        @JvmStatic
        fun of(level: Level, loc: BlockPos): Loc4 = Loc4Level(level, loc)

        @JvmStatic
        fun of(level: ILevel, loc: BlockPos): Loc4 = Loc4ILevel(level, loc)

        const val X_KEY = "x"
        const val Y_KEY = "y"
        const val Z_KEY = "z"
        const val DIM_KEY = "dim"

        @JvmField
        val ORIGIN: Loc4 = of(Identifier.parse("overworld"), BlockPos(0, 0, 0))

        /**
         * Serializes any Loc4 by value; decodes to a [Loc4Indirect].
         */
        @JvmField
        val CODEC: Codec<Loc4> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf(X_KEY).forGetter(Loc4::x),
                Codec.INT.fieldOf(Y_KEY).forGetter(Loc4::y),
                Codec.INT.fieldOf(Z_KEY).forGetter(Loc4::z),
                Identifier.CODEC.fieldOf(DIM_KEY).forGetter(Loc4::dimensionId),
            ).apply(instance) { x, y, z, dim -> of(dim, BlockPos(x, y, z)) }
        }
    }
}
