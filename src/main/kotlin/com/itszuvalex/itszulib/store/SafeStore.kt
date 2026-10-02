package com.itszuvalex.itszulib.store

import com.mojang.logging.LogUtils
import com.mojang.serialization.DynamicOps
import net.minecraft.nbt.CompoundTag
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
 * How a [SafeStore] turns its value into NBT and back.
 */
interface StoreFormat<T> {
    /** The value of a store with nothing saved yet (a new world). */
    val empty: T

    fun encode(value: T, ops: DynamicOps<Tag>): CompoundTag

    /**
     * Decodes strictly: throw on anything unreadable or partly unreadable, rather than dropping it. [source] is the
     * file being read, for log messages.
     */
    fun decode(tag: CompoundTag, ops: DynamicOps<Tag>, source: Path): T
}

/**
 * Persists one immutable value to [file], next to which it keeps `<file>.bak` (the previous good save) and, while
 * writing, `<file>.tmp`. For server data that must not be lost (teams, registries of places, anything players build
 * up), in place of vanilla `SavedData`.
 *
 * Vanilla `SavedData`, when it fails to read saved data, hands out a fresh empty instance and later saves that over the
 * unreadable file, wiping it. Here:
 * - [load] reads the file and, if it is unreadable, falls back to the backup; the unreadable file is then moved aside
 *   (`<file>.corrupt-<time>`) instead of being overwritten.
 * - If neither the file nor its backup can be read, both are left untouched and the store is not [writable]: the
 *   server runs with the empty value in memory and [save] refuses to write, so nothing on disk is lost. An
 *   administrator restores the data by hand.
 * - [save] writes to the temporary file, reads it back and decodes it to prove it is complete, copies the current
 *   file to the backup, then atomically moves the temporary file into place. A crash at any step leaves a readable
 *   file or backup.
 *
 * @param name What the data is, for log messages.
 */
open class SafeStore<T>(val file: Path, val format: StoreFormat<T>, val name: String = file.fileName.toString()) {
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
    fun load(ops: DynamicOps<Tag>): T {
        writable = false
        val primary = if (Files.exists(file)) read(file, ops) else null
        if (primary != null && primary.error == null) return primary.value.also { writable = true }

        val fromBackup = if (Files.exists(backup)) read(backup, ops) else null
        if (fromBackup != null && fromBackup.error == null) {
            if (primary != null) {
                // The next save replaces the unreadable file; keep a copy of it for inspection.
                val aside = file.resolveSibling("${file.fileName}.corrupt-${LocalDateTime.now().format(STAMP)}")
                runCatching { Files.move(file, aside) }
                    .onSuccess { LOGGER.error("Saved {} {} is unreadable ({}); moved it to {}", name, file, primary.error, aside) }
                    .onFailure {
                        LOGGER.error("Saved {} {} is unreadable ({}) and could not be moved aside", name, file, primary.error, it)
                        return format.empty
                    }
            }
            LOGGER.warn("Loaded {} from backup {}", name, backup)
            return fromBackup.value.also { writable = true }
        }
        if (primary == null && fromBackup == null) {
            // A new world: nothing saved yet.
            return format.empty.also { writable = true }
        }
        // Both unreadable: leave both files exactly as they are, so this keeps failing loudly until restored.
        LOGGER.error(
            "Could not read {} from {} or its backup ({}). It will not be saved this session, so the files on disk are " +
                "left untouched; restore them by hand.",
            name, file, fromBackup?.error ?: "no backup",
        )
        return format.empty
    }

    /**
     * Writes [value] atomically. Does nothing if the store is not [writable].
     *
     * @return Whether the value was written.
     */
    fun save(value: T, ops: DynamicOps<Tag>): Boolean {
        if (!writable) {
            LOGGER.warn("Not saving {} to {}: the saved data could not be read at load", name, file)
            return false
        }
        val tag = format.encode(value, ops)
        Files.createDirectories(file.toAbsolutePath().parent)
        NbtIo.writeCompressed(tag, temp)
        // Prove the written file is complete before it replaces anything.
        val check = read(temp, ops)
        if (check.error != null) {
            Files.deleteIfExists(temp)
            throw IllegalStateException("$name written to $temp does not read back: ${check.error}")
        }
        if (Files.exists(file)) Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING)
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
        }
        return true
    }

    private class ReadResult<T>(val value: T, val error: String?)

    private fun read(path: Path, ops: DynamicOps<Tag>): ReadResult<T> = try {
        ReadResult(format.decode(NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()), ops, path), null)
    } catch (e: Exception) {
        ReadResult(format.empty, e.toString())
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
        private val STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
