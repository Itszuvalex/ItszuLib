package com.itszuvalex.itszulib.store

import com.mojang.serialization.DynamicOps
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * A store of names, standing in for any mod's server data.
 */
private object Names : StoreFormat<List<String>> {
    override val empty: List<String> = emptyList()

    override fun encode(value: List<String>, ops: DynamicOps<Tag>): CompoundTag =
        CompoundTag().apply { putString("names", value.joinToString(",")) }

    override fun decode(tag: CompoundTag, ops: DynamicOps<Tag>, source: Path): List<String> {
        val names = tag.getString("names").orElseThrow { IllegalArgumentException("no names in $source") }
        return if (names.isEmpty()) emptyList() else names.split(",")
    }
}

class SafeStoreTests {
    @TempDir
    lateinit var dir: Path

    private val ops = NbtOps.INSTANCE

    private fun manager() = StoreManager(Names.empty).also { it.load(SafeStore(dir.resolve("names.dat"), Names), ops) }

    @Test
    fun ChangeAndSave_ReachesAFreshManager() {
        val first = manager()
        first.change { it + "ann" }
        first.save()
        Assertions.assertEquals(listOf("ann"), manager().state)
    }

    @Test
    fun UnreadableData_NotPersistent_FileUntouched() {
        Files.write(dir.resolve("names.dat"), byteArrayOf(3, 1, 4))
        val manager = manager()
        Assertions.assertFalse(manager.isPersistent)
        manager.change { it + "bo" }
        manager.save()
        Assertions.assertArrayEquals(byteArrayOf(3, 1, 4), Files.readAllBytes(dir.resolve("names.dat")))
    }

    @Test
    fun FailedOperation_ChangesNothing() {
        val manager = manager()
        manager.change { listOf("ann") }
        Assertions.assertThrows(IllegalStateException::class.java) { manager.change { error("refused") } }
        Assertions.assertEquals(listOf("ann"), manager.state)
    }
}
