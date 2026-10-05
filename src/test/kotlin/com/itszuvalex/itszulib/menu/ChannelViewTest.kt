package com.itszuvalex.itszulib.menu

import com.itszuvalex.itszulib.channel.ChannelScope
import io.netty.buffer.Unpooled
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.Identifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class ChannelViewTest {
    private fun roundTrip(view: ChannelView): ChannelView {
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
        ChannelView.CODEC.encode(buf, view)
        return ChannelView.CODEC.decode(buf)
    }

    @Test
    fun Codec_Empty_RoundTrips() {
        assertEquals(ChannelView.EMPTY, roundTrip(ChannelView.EMPTY))
    }

    @Test
    fun Codec_FullView_RoundTrips() {
        val view = ChannelView(
            listOf(Identifier.parse("test:power"), Identifier.parse("test:items")), 1, ChannelScope.TEAM, true,
            listOf(ChannelEntry(UUID(1, 2), "Base", 3), ChannelEntry(UUID(3, 4), "x".repeat(32), 0)),
            UUID(1, 2), "Base", ChannelScope.PLAYER, false, "There is already a channel called \"Base\".",
        )
        assertEquals(view, roundTrip(view))
        assertEquals(Identifier.parse("test:items"), view.resource)
    }

    @Test
    fun Codec_BoundToAChannelThatIsGone_RoundTrips() {
        val view = ChannelView.EMPTY.copy(resources = listOf(Identifier.parse("test:power")), currentId = null, currentName = "Gone", currentMissing = true)
        assertEquals(view, roundTrip(view))
    }

    @Test
    fun Resource_IndexPastTheEnd_IsNull() {
        assertEquals(null, ChannelView.EMPTY.copy(resourceIndex = 5).resource)
    }
}
