package com.itszuvalex.itszulib.menu

import com.itszuvalex.itszulib.TestableIFluidStack
import com.itszuvalex.itszulib.api.storage.FluidStorageArray
import io.netty.buffer.Unpooled
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A menu without a type or player, holding plain values.
 */
private class ValueMenu(containerId: Int) : MenuCore(null, containerId, null) {
    var power = 0.0
    var label = ""
    var ticks = 0
    init {
        addSync(MenuSyncs.double({ power }, { power = it }))
        addSync(MenuSyncs.string({ label }, { label = it }))
        addSync(MenuSyncs.int({ ticks }, { ticks = it }))
    }

    override fun stillValid(player: Player): Boolean = true

    override fun quickMoveStack(player: Player, index: Int): ItemStack = ItemStack.EMPTY
}

class MenuSyncTest {
    @Test
    fun Poll_FirstAlwaysChanged_ThenOnlyOnChange() {
        var value = 5
        val sync = MenuSyncs.int({ value }, { })
        assertTrue(sync.poll())
        assertFalse(sync.poll())
        value = 6
        assertTrue(sync.poll())
        sync.reset()
        assertTrue(sync.poll())
    }

    @Test
    fun Poll_MutableValue_ComparedAgainstACopy() {
        val tanks = FluidStorageArray(1, 1000)
        tanks.set(0, TestableIFluidStack(1, 100))
        val sync = MenuSyncs.fluid(tanks, 0)
        assertTrue(sync.poll())
        // Mutating the stored stack in place must still count as a change.
        tanks.get(0).setAmount(200)
        assertTrue(sync.poll())
        assertFalse(sync.poll())
    }

    @Test
    fun EncodeDecode_RoundTrips() {
        val sync = MenuSyncs.double({ 12.5 }, { })
        var received = 0.0
        val client = MenuSyncs.double({ received }, { received = it })
        sync.poll()
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
        sync.encode(buf)
        client.decode(buf)
        assertEquals(12.5, received)
        // The decoded value counts as the last one, so polling the client copy reports no change.
        assertFalse(client.poll())
    }
}

class MenuPayloadTest {
    private fun <T : Any> roundTrip(value: T, codec: net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, T>): T {
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
        codec.encode(buf, value)
        return codec.decode(buf)
    }

    @Test
    fun SyncPayload_StreamCodecRoundTrips() {
        val payload = MenuSyncPayload(7, listOf(MenuSyncPayload.Entry(0, byteArrayOf(1, 2)), MenuSyncPayload.Entry(3, byteArrayOf())))
        assertEquals(payload, roundTrip(payload, MenuSyncPayload.STREAM_CODEC))
    }

    @Test
    fun ActionPayload_StreamCodecRoundTrips() {
        val payload = MenuActionPayload(3, 12, -4)
        assertEquals(payload, roundTrip(payload, MenuActionPayload.STREAM_CODEC))
    }
}

class MenuCoreSyncTest {
    @Test
    fun CollectApply_CopiesEveryValueToTheClientMenu() {
        val server = ValueMenu(4)
        val client = ValueMenu(4)
        server.power = 3.5
        server.label = "hi"
        server.ticks = 9

        val payload = server.collectSyncPayload(RegistryAccess.EMPTY, all = true)
        assertNotNull(payload)
        assertTrue(client.applySyncPayload(payload!!, RegistryAccess.EMPTY))
        assertEquals(3.5, client.power)
        assertEquals("hi", client.label)
        assertEquals(9, client.ticks)
    }

    @Test
    fun Collect_OnlyChangedValues_NullWhenNothingChanged() {
        val server = ValueMenu(1)
        server.collectSyncPayload(RegistryAccess.EMPTY, all = false)
        assertNull(server.collectSyncPayload(RegistryAccess.EMPTY, all = false))
        server.ticks = 2
        val payload = server.collectSyncPayload(RegistryAccess.EMPTY, all = false)!!
        assertEquals(listOf(2), payload.entries.map { it.index })
        // "all" resends everything even when unchanged.
        assertEquals(3, server.collectSyncPayload(RegistryAccess.EMPTY, all = true)!!.entries.size)
    }

    /**
     * Regression: 1.12.2 applied a sync to whatever open container had the same GUI id, so a value meant for a menu
     * that was just closed could land in a newly opened one of the same type.
     */
    @Test
    fun Apply_OtherContainerId_Ignored() {
        val server = ValueMenu(1)
        val client = ValueMenu(2)
        server.ticks = 5
        assertFalse(client.applySyncPayload(server.collectSyncPayload(RegistryAccess.EMPTY, all = true)!!, RegistryAccess.EMPTY))
        assertEquals(0, client.ticks)
    }

    @Test
    fun Apply_UnknownSyncIndex_Ignored() {
        val client = ValueMenu(1)
        val payload = MenuSyncPayload(1, listOf(MenuSyncPayload.Entry(99, byteArrayOf(1))))
        assertTrue(client.applySyncPayload(payload, RegistryAccess.EMPTY))
    }

    @Test
    fun AddSync_AssignsIndicesInOrder() {
        assertEquals(listOf(0, 1, 2), ValueMenu(1).syncs().map { it.index })
    }
}
