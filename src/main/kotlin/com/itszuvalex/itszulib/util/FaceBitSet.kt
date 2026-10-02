package com.itszuvalex.itszulib.util

import net.minecraft.core.Direction

/**
 * A set of block faces, plus "no face" (null), packed into the low 7 bits of an int: bit `face.ordinal` for each
 * [Direction], bit 6 for null. Port of ItszuLib 1.12.2's `FaceBitSet` (which kept null at bit 7 of a signed byte).
 */
class FaceBitSet @JvmOverloads constructor(bits: Int = 0) {
    /**
     * The packed bits; persist this value.
     */
    var bits: Int = bits and ALL
        private set

    operator fun get(face: Direction?): Boolean = bits and mask(face) != 0

    fun set(face: Direction?) {
        bits = bits or mask(face)
    }

    fun clear(face: Direction?) {
        bits = bits and mask(face).inv()
    }

    fun toggle(face: Direction?) {
        bits = bits xor mask(face)
    }

    operator fun set(face: Direction?, value: Boolean) = if (value) set(face) else clear(face)

    fun clearAll() {
        bits = 0
    }

    /**
     * Replaces the contents with [bits] (unknown bits are dropped).
     */
    fun load(bits: Int) {
        this.bits = bits and ALL
    }

    fun isEmpty(): Boolean = bits == 0

    /**
     * The non-null faces in the set, in [Direction] order.
     */
    fun faces(): List<Direction> = Direction.entries.filter { get(it) }

    override fun equals(other: Any?): Boolean = other is FaceBitSet && other.bits == bits

    override fun hashCode(): Int = bits

    override fun toString(): String = "FaceBitSet${faces()}${if (get(null)) "+null" else ""}"

    companion object {
        private const val NULL_BIT = 6
        private const val ALL = (1 shl (NULL_BIT + 1)) - 1

        private fun mask(face: Direction?): Int = 1 shl (face?.ordinal ?: NULL_BIT)
    }
}
