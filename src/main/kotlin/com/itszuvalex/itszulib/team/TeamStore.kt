package com.itszuvalex.itszulib.team

import com.mojang.logging.LogUtils
import com.mojang.serialization.DynamicOps
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.Tag
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Persists a [TeamState] to [file], next to which it keeps `<file>.bak` (the previous good save) and, while writing,
 * `<file>.tmp`.
 *
 * Deliberately not vanilla `SavedData`: when vanilla fails to read saved data it hands out a fresh, empty instance and
 * later saves that empty instance over the unreadable file, wiping it. Here:
 * - [load] reads the file and, if it is unreadable, falls back to the backup; the unreadable file is then moved aside
 *   (`<file>.corrupt-<time>`) instead of being overwritten.
 * - If neither the file nor its backup can be read, both are left untouched and the store is not [writable]: the
 *   server runs with empty teams in memory and [save] refuses to write, so nothing on disk is lost. An administrator
 *   restores the data by hand.
 * - [save] writes to the temporary file, reads it back and decodes it to prove it is complete, copies the current
 *   file to the backup, then atomically moves the temporary file into place. A crash at any step leaves a readable
 *   file or backup.
 */
class TeamStore(val file: Path) {
    val backup: Path = file.resolveSibling(file.fileName.toString() + ".bak")
    private val temp: Path = file.resolveSibling(file.fileName.toString() + ".tmp")

    /**
     * False after a load that could read neither the file nor its backup; [save] then does nothing.
     */
    var writable: Boolean = false
        private set

    /**
     * Never throws: a failure is logged and leaves the store read-only (see [writable]).
     */
    fun load(ops: DynamicOps<Tag>): TeamState {
        writable = false
        val primary = if (Files.exists(file)) read(file, ops) else null
        if (primary?.state != null) return primary.state.also { writable = true }

        val fromBackup = if (Files.exists(backup)) read(backup, ops) else null
        if (fromBackup?.state != null) {
            if (primary != null) {
                // The next save replaces the unreadable file; keep a copy of it for inspection.
                val aside = file.resolveSibling("${file.fileName}.corrupt-${LocalDateTime.now().format(STAMP)}")
                runCatching { Files.move(file, aside) }
                    .onSuccess { LOGGER.error("Team data {} is unreadable ({}); moved it to {}", file, primary.error, aside) }
                    .onFailure {
                        LOGGER.error("Team data {} is unreadable ({}) and could not be moved aside", file, primary.error, it)
                        return TeamState.EMPTY
                    }
            }
            LOGGER.warn("Loaded team data from backup {}", backup)
            return fromBackup.state.also { writable = true }
        }
        if (primary == null && fromBackup == null) {
            // A new world: nothing saved yet.
            return TeamState.EMPTY.also { writable = true }
        }
        // Both unreadable: leave both files exactly as they are, so this keeps failing loudly until restored.
        LOGGER.error(
            "Could not read team data from {} or its backup ({}). Teams will not be saved this session, so the files " +
                "on disk are left untouched; restore them by hand.",
            file, fromBackup?.error ?: "no backup",
        )
        return TeamState.EMPTY
    }

    /**
     * Writes [state] atomically. Does nothing if the store is not [writable].
     *
     * @return Whether the state was written.
     */
    fun save(state: TeamState, ops: DynamicOps<Tag>): Boolean {
        if (!writable) {
            LOGGER.warn("Not saving team data to {}: the saved data could not be read at load", file)
            return false
        }
        val tag = TeamCodec.encode(state, ops)
        Files.createDirectories(file.toAbsolutePath().parent)
        NbtIo.writeCompressed(tag, temp)
        // Prove the written file is complete before it replaces anything.
        val check = read(temp, ops)
        if (check.state == null) {
            Files.deleteIfExists(temp)
            throw IllegalStateException("Team data written to $temp does not read back: ${check.error}")
        }
        if (Files.exists(file)) Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING)
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
        }
        return true
    }

    private class ReadResult(val state: TeamState?, val error: String?)

    private fun read(path: Path, ops: DynamicOps<Tag>): ReadResult = try {
        val teams = TeamCodec.decode(NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()), ops)
        ReadResult(TeamState.repaired(teams) { LOGGER.warn("Repaired team data from {}: {}", path, it) }, null)
    } catch (e: Exception) {
        ReadResult(null, e.toString())
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
        private val STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
