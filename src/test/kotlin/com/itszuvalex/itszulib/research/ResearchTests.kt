package com.itszuvalex.itszulib.research

import com.itszuvalex.itszulib.team.Research
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.StringTag
import net.minecraft.resources.Identifier
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private fun id(path: String) = Identifier.fromNamespaceAndPath("itszulib_test", path)

class ResearchTests {
    @Test
    fun Codec_OldUnlockedOnlyList_Decodes() {
        val old = ListTag().apply { add(StringTag.valueOf(id("a").toString())) }
        val research = Research.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow()
        Assertions.assertEquals(Research(setOf(id("a"))), research)
    }

    @Test
    fun Codec_WithProgress_RoundTrips() {
        val research = Research(setOf(id("a")), mapOf(id("b") to 40L))
        val tag = Research.CODEC.encodeStart(NbtOps.INSTANCE, research).getOrThrow()
        Assertions.assertEquals(research, Research.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow())
    }

    @Test
    fun Codec_ProgressForUnlockedOrNotPositive_IsRejected() {
        for (bad in listOf(mapOf(id("a") to 5L), mapOf(id("b") to 0L), mapOf(id("b") to -3L))) {
            val tag = Research.CODEC.encodeStart(NbtOps.INSTANCE, Research(setOf(id("a")))).getOrThrow() as net.minecraft.nbt.CompoundTag
            val progress = net.minecraft.nbt.CompoundTag()
            for ((k, v) in bad) progress.putLong(k.toString(), v)
            tag.put("progress", progress)
            Assertions.assertTrue(Research.CODEC.parse(NbtOps.INSTANCE, tag).isError, "accepted $bad")
        }
    }

    @Test
    fun Unlock_DropsProgress() {
        val research = Research(emptySet(), mapOf(id("a") to 10L)).unlock(id("a"))
        Assertions.assertTrue(research.has(id("a")))
        Assertions.assertEquals(0L, research.progressOf(id("a")))
    }

    @Test
    fun WithProgress_UnlockedIdOrZero_KeepsNone() {
        val research = Research(setOf(id("a")))
        Assertions.assertSame(research, research.withProgress(id("a"), 10L))
        Assertions.assertEquals(Research(setOf(id("a"))), Research(setOf(id("a")), mapOf(id("b") to 4L)).withProgress(id("b"), 0L))
    }

    @Test
    fun Merge_UnionsUnlockedAndKeepsLargerProgress() {
        val team = Research(setOf(id("a")), mapOf(id("b") to 10L, id("c") to 3L))
        val joining = Research(setOf(id("c")), mapOf(id("b") to 25L, id("d") to 1L))
        Assertions.assertEquals(Research(setOf(id("a"), id("c")), mapOf(id("b") to 25L, id("d") to 1L)), Research.merge(team, joining))
    }

    @Test
    fun Queue_NoDuplicatesOrUnlocked() {
        val research = Research(setOf(id("done"))).withQueue(listOf(id("a"), id("done"), id("b"), id("a")))
        Assertions.assertEquals(listOf(id("a"), id("b")), research.queue)
        Assertions.assertEquals(listOf(id("a"), id("b"), id("c")), research.enqueue(listOf(id("b"), id("c"))).queue)
        Assertions.assertEquals(listOf(id("b")), research.unqueue(listOf(id("a"))).queue)
        Assertions.assertThrows(IllegalArgumentException::class.java) { Research(emptySet(), emptyMap(), listOf(id("a"), id("a"))) }
        Assertions.assertThrows(IllegalArgumentException::class.java) { Research(setOf(id("a")), emptyMap(), listOf(id("a"))) }
    }

    @Test
    fun Unlock_TakesOffQueue() {
        val research = Research.EMPTY.withQueue(listOf(id("a"), id("b"))).unlock(id("a"))
        Assertions.assertEquals(listOf(id("b")), research.queue)
    }

    @Test
    fun Codec_WithQueue_RoundTripsInOrder() {
        val research = Research(setOf(id("a")), mapOf(id("c") to 2L), listOf(id("z"), id("c"), id("m")))
        val tag = Research.CODEC.encodeStart(NbtOps.INSTANCE, research).getOrThrow()
        Assertions.assertEquals(research, Research.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow())
    }

    @Test
    fun Merge_TeamQueueFirstThenJoinersAdditions() {
        val team = Research(setOf(id("x")), emptyMap(), listOf(id("a"), id("b")))
        val joining = Research(setOf(id("b")), emptyMap(), listOf(id("c"), id("a"), id("x")))
        Assertions.assertEquals(listOf(id("a"), id("c")), Research.merge(team, joining).queue)
    }
}
