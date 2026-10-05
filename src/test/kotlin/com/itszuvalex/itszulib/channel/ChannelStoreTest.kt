package com.itszuvalex.itszulib.channel

import net.minecraft.nbt.NbtOps
import net.minecraft.resources.Identifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.UUID

class ChannelStoreTest {
    private val source = Path.of("channels.dat")
    private val alice = UUID.randomUUID()

    private fun state() = ChannelState.EMPTY
        .create(alice, UUID.randomUUID(), ChannelScope.PLAYER, Identifier.parse("test:power"), "mine")
        .create(alice, UUID.randomUUID(), ChannelScope.TEAM, Identifier.parse("test:items"), "base")

    @Test
    fun Format_RoundTrips() {
        val saved = ChannelStore.FORMAT.encode(state(), NbtOps.INSTANCE)
        assertEquals(state().channels.size, ChannelStore.FORMAT.decode(saved, NbtOps.INSTANCE, source).channels.size)
        val original = state()
        assertEquals(original, ChannelStore.FORMAT.decode(ChannelStore.FORMAT.encode(original, NbtOps.INSTANCE), NbtOps.INSTANCE, source))
    }

    @Test
    fun Format_Empty_RoundTrips() {
        assertEquals(ChannelState.EMPTY, ChannelStore.FORMAT.decode(ChannelStore.FORMAT.encode(ChannelState.EMPTY, NbtOps.INSTANCE), NbtOps.INSTANCE, source))
    }

    @Test
    fun Decode_UnknownVersionOrMissingChannels_Throws() {
        val saved = ChannelStore.FORMAT.encode(state(), NbtOps.INSTANCE)
        saved.putInt("version", 99)
        assertThrows(IllegalArgumentException::class.java) { ChannelStore.FORMAT.decode(saved, NbtOps.INSTANCE, source) }
        val missing = ChannelStore.FORMAT.encode(state(), NbtOps.INSTANCE)
        missing.remove("channels")
        assertThrows(IllegalArgumentException::class.java) { ChannelStore.FORMAT.decode(missing, NbtOps.INSTANCE, source) }
    }

    @Test
    fun Decode_ChannelsBreakingTheRules_Throws() {
        val saved = ChannelStore.FORMAT.encode(state(), NbtOps.INSTANCE)
        val list = saved.getListOrEmpty("channels")
        list.add(list.get(0).copy())
        saved.put("channels", list)
        assertThrows(Exception::class.java) { ChannelStore.FORMAT.decode(saved, NbtOps.INSTANCE, source) }
    }
}
