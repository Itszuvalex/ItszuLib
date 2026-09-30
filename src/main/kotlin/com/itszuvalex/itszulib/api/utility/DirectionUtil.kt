package com.itszuvalex.itszulib.api.utility

import net.minecraft.core.Direction

object DirectionUtil {
    @JvmField
    var DEFAULT_HORIZONTAL_FACING: Direction = Direction.NORTH

    /**
     * @param relative Direction relative to a block facing north (e.g. [Direction.EAST] = the block's right side).
     * @param front The block's actual horizontal facing.
     * @return The world direction of that side.
     */
    @JvmStatic
    fun getAbsoluteDirectionFromHorizontalRelative(relative: Direction, front: Direction): Direction = when (relative) {
        Direction.UP -> Direction.UP
        Direction.DOWN -> Direction.DOWN
        else -> when (front) {
            Direction.EAST -> relative.clockWise
            Direction.SOUTH -> relative.opposite
            Direction.WEST -> relative.counterClockWise
            else -> relative
        }
    }

    /**
     * Inverse of [getAbsoluteDirectionFromHorizontalRelative].
     */
    @JvmStatic
    fun getHorizontalRelativeDirectionFromAbsolute(absolute: Direction, front: Direction): Direction = when (absolute) {
        Direction.UP -> Direction.UP
        Direction.DOWN -> Direction.DOWN
        else -> when (front) {
            Direction.EAST -> absolute.counterClockWise
            Direction.SOUTH -> absolute.opposite
            Direction.WEST -> absolute.clockWise
            else -> absolute
        }
    }
}
