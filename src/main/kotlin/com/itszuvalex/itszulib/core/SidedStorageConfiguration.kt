package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.storage.IFluidStorage
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.utility.DirectionUtil
import net.minecraft.core.Direction
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.neoforge.common.util.ValueIOSerializable

enum class EnumAutomaticIO {
    NONE,
    INPUT,
    OUTPUT,
}

/**
 * Maps each block face (relative to the block's horizontal [front]) to one of several named storages, plus an
 * automatic IO mode per face.
 *
 * Persisted as `"<faceOrdinal>" -> storage name` and `"io<faceOrdinal>" -> EnumAutomaticIO name`, where the face is
 * relative to a north-facing block.
 *
 * The per-face accessors are open so a subclass can override faces, e.g. a multiblock part hiding the faces that touch
 * the rest of its multiblock.
 */
abstract class SidedStorageConfiguration<T : Any>(
    private val defaults: (Direction) -> String,
    private val storages: Map<String, T>,
    val front: () -> Direction,
) : ValueIOSerializable {
    private val storageSegments = Array(Direction.entries.size) { defaults(Direction.entries[it]) }
    private val automaticIO = Array(Direction.entries.size) { EnumAutomaticIO.NONE }

    open fun getStorageForGlobalFacing(direction: Direction): T? = storages[getStorageNameForAbsoluteFacing(direction)]

    open fun getStorageForRelativeFacing(direction: Direction): T? = storages[getStorageNameForRelativeFacing(direction)]

    open fun cycleRelativeFacingStorageForward(direction: Direction) = cycleRelativeFacingStorage(direction, true)

    open fun cycleRelativeFacingStorageBackward(direction: Direction) = cycleRelativeFacingStorage(direction, false)

    open fun cycleRelativeFacingIOForward(direction: Direction) = cycleRelativeFacingIO(direction, true)

    open fun cycleRelativeFacingIOBackward(direction: Direction) = cycleRelativeFacingIO(direction, false)

    open fun getStorageNameForRelativeFacing(direction: Direction): String = storageSegments[direction.ordinal]

    open fun getStorageNameForAbsoluteFacing(direction: Direction): String =
        storageSegments[DirectionUtil.getHorizontalRelativeDirectionFromAbsolute(direction, front()).ordinal]

    open fun getIOForRelativeFacing(direction: Direction): EnumAutomaticIO = automaticIO[direction.ordinal]

    open fun getIOForAbsoluteFacing(direction: Direction): EnumAutomaticIO =
        automaticIO[DirectionUtil.getHorizontalRelativeDirectionFromAbsolute(direction, front()).ordinal]

    private fun cycleRelativeFacingStorage(dir: Direction, forward: Boolean) {
        val keys = storages.keys.sorted()
        if (keys.isEmpty()) return
        var invInd = keys.indexOf(storageSegments[dir.ordinal])
        // Unknown key: forward lands on the first key, backward on the last.
        if (invInd < 0) invInd = if (forward) -1 else 0
        val shift = if (forward) 1 else -1
        storageSegments[dir.ordinal] = keys[(invInd + shift + keys.size) % keys.size]
    }

    private fun cycleRelativeFacingIO(dir: Direction, forward: Boolean) {
        val keys = EnumAutomaticIO.entries
        val shift = if (forward) 1 else -1
        automaticIO[dir.ordinal] = keys[(automaticIO[dir.ordinal].ordinal + shift + keys.size) % keys.size]
    }

    override fun serialize(output: ValueOutput) {
        for (i in storageSegments.indices) {
            output.putString(i.toString(), storageSegments[i])
            output.putString("io$i", automaticIO[i].name)
        }
    }

    /**
     * Missing or unknown values fall back to the defaults rather than failing, so a storage that was renamed or
     * removed, or corrupt data, can't crash loading.
     */
    override fun deserialize(input: ValueInput) {
        for (i in storageSegments.indices) {
            storageSegments[i] = input.getString(i.toString()).filter(storages::containsKey)
                .orElseGet { defaults(Direction.entries[i]) }
            automaticIO[i] = readIO(input, "io$i")
        }
    }

    private fun readIO(input: ValueInput, key: String): EnumAutomaticIO {
        val values = EnumAutomaticIO.entries
        input.getString(key).orElse(null)?.let { name -> values.firstOrNull { it.name == name }?.let { return it } }
        // Older saves stored the ordinal.
        input.getInt(key).orElse(null)?.let { o -> if (o in values.indices) return values[o] }
        return EnumAutomaticIO.NONE
    }
}

open class SidedItemStorageConfiguration(
    defaults: (Direction) -> String,
    storages: Map<String, IItemStorage>,
    front: () -> Direction,
) : SidedStorageConfiguration<IItemStorage>(defaults, storages, front)

open class SidedFluidStorageConfiguration(
    defaults: (Direction) -> String,
    storages: Map<String, IFluidStorage>,
    front: () -> Direction,
) : SidedStorageConfiguration<IFluidStorage>(defaults, storages, front)
