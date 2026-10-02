package com.itszuvalex.itszulib.team

import com.itszuvalex.itszulib.store.SafeStore
import com.itszuvalex.itszulib.store.StoreFormat
import com.mojang.logging.LogUtils
import com.mojang.serialization.DynamicOps
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import java.nio.file.Path

/**
 * Persists a [TeamState] to [file] through a [SafeStore] (backup fallback, strict decoding, refusal to save over
 * unreadable data, verified atomic writes). Decoding also checks the teams' invariants, repairing what can be
 * repaired safely (see [TeamState.repaired]).
 */
class TeamStore(file: Path) : SafeStore<TeamState>(file, FORMAT, "team data") {
    companion object {
        private val LOGGER = LogUtils.getLogger()

        @JvmField
        val FORMAT: StoreFormat<TeamState> = object : StoreFormat<TeamState> {
            override val empty: TeamState get() = TeamState.EMPTY

            override fun encode(value: TeamState, ops: DynamicOps<Tag>): CompoundTag = TeamCodec.encode(value, ops)

            override fun decode(tag: CompoundTag, ops: DynamicOps<Tag>, source: Path): TeamState =
                TeamState.repaired(TeamCodec.decode(tag, ops)) { LOGGER.warn("Repaired team data from {}: {}", source, it) }
        }
    }
}
