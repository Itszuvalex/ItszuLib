package com.itszuvalex.itszulib.channel

import com.mojang.serialization.JsonOps
import net.minecraft.resources.Identifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ChannelStateTest {
    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()
    private val teamA = UUID.randomUUID()
    private val teamB = UUID.randomUUID()
    private val power = Identifier.parse("test:power")
    private val items = Identifier.parse("test:items")
    private var next = 0L
    private val ids: () -> UUID = { UUID(0, ++next) }

    private fun ChannelState.id(name: String) = channels.values.single { it.name == name }.id

    @Test
    fun Create_PlayerChannel_VisibleToThatPlayerOnly() {
        val state = ChannelState.EMPTY.create(alice, teamA, ChannelScope.PLAYER, power, "mine", ids)
        assertEquals(listOf("mine"), state.visible(alice, teamA, power, ChannelScope.PLAYER).map { it.name })
        assertTrue(state.visible(bob, teamA, power, ChannelScope.PLAYER).isEmpty())
        assertTrue(state.visible(alice, teamA, power, ChannelScope.TEAM).isEmpty())
        assertTrue(state.visible(alice, teamA, items, ChannelScope.PLAYER).isEmpty())
    }

    @Test
    fun Create_TeamChannel_VisibleToTheTeamNotToOthers() {
        val state = ChannelState.EMPTY.create(alice, teamA, ChannelScope.TEAM, power, "base", ids)
        assertEquals(listOf("base"), state.visible(bob, teamA, power, ChannelScope.TEAM).map { it.name })
        assertTrue(state.visible(bob, teamB, power, ChannelScope.TEAM).isEmpty())
    }

    @Test
    fun Create_NamesAreTrimmedAndSortedIgnoringCase() {
        val state = ChannelState.EMPTY
            .create(alice, teamA, ChannelScope.PLAYER, power, "  b ", ids)
            .create(alice, teamA, ChannelScope.PLAYER, power, "A", ids)
        assertEquals(listOf("A", "b"), state.visible(alice, teamA, power, ChannelScope.PLAYER).map { it.name })
    }

    @Test
    fun Create_BadNames_Refused() {
        for (name in listOf("", "   ", "x".repeat(33), "tab\there")) {
            assertThrows(ChannelException::class.java) { ChannelState.EMPTY.create(alice, teamA, ChannelScope.PLAYER, power, name, ids) }
        }
        ChannelState.EMPTY.create(alice, teamA, ChannelScope.PLAYER, power, "x".repeat(32), ids)
    }

    @Test
    fun Create_SameNameSameOwnerAndResource_RefusedIgnoringCase() {
        val state = ChannelState.EMPTY.create(alice, teamA, ChannelScope.PLAYER, power, "Base", ids)
        assertThrows(ChannelException::class.java) { state.create(alice, teamA, ChannelScope.PLAYER, power, "base", ids) }
    }

    @Test
    fun Create_SameNameElsewhere_Allowed() {
        val state = ChannelState.EMPTY.create(alice, teamA, ChannelScope.PLAYER, power, "base", ids)
            .create(alice, teamA, ChannelScope.PLAYER, items, "base", ids)
            .create(bob, teamA, ChannelScope.PLAYER, power, "base", ids)
            .create(alice, teamA, ChannelScope.TEAM, power, "base", ids)
        assertEquals(4, state.channels.size)
    }

    @Test
    fun Create_TeamChannelWithoutATeam_Refused() {
        assertThrows(ChannelException::class.java) { ChannelState.EMPTY.create(alice, null, ChannelScope.TEAM, power, "x", ids) }
    }

    @Test
    fun Delete_OwnPlayerChannel_Removes() {
        val state = ChannelState.EMPTY.create(alice, teamA, ChannelScope.PLAYER, power, "mine", ids)
        assertTrue(state.delete(alice, teamA, state.id("mine")).channels.isEmpty())
    }

    @Test
    fun Delete_SomeoneElsesPlayerChannel_Refused() {
        val state = ChannelState.EMPTY.create(alice, teamA, ChannelScope.PLAYER, power, "mine", ids)
        assertThrows(ChannelException::class.java) { state.delete(bob, teamA, state.id("mine")) }
    }

    @Test
    fun Delete_TeamChannel_AnyMemberMayOthersMayNot() {
        val state = ChannelState.EMPTY.create(alice, teamA, ChannelScope.TEAM, power, "base", ids)
        assertThrows(ChannelException::class.java) { state.delete(bob, teamB, state.id("base")) }
        assertTrue(state.delete(bob, teamA, state.id("base")).channels.isEmpty())
    }

    @Test
    fun Delete_UnknownChannel_Refused() {
        assertThrows(ChannelException::class.java) { ChannelState.EMPTY.delete(alice, teamA, UUID.randomUUID()) }
    }

    private fun bind(player: UUID, scope: ChannelScope, name: String) = ChannelBinding(player, scope, name)

    @Test
    fun Resolve_PlayerChannel_FollowsTheOwner() {
        val state = ChannelState.EMPTY.create(alice, teamA, ChannelScope.PLAYER, power, "mine", ids)
        assertEquals("mine", state.resolve(bind(alice, ChannelScope.PLAYER, "mine"), power) { teamA }?.name)
        assertNull(state.resolve(bind(bob, ChannelScope.PLAYER, "mine"), power) { teamA }, "another player's binding does not reach it")
    }

    @Test
    fun Resolve_TeamChannel_LostWhenTheOwnerLeavesTheTeam() {
        val state = ChannelState.EMPTY.create(alice, teamA, ChannelScope.TEAM, power, "base", ids)
        val binding = bind(alice, ChannelScope.TEAM, "base")
        assertEquals("base", state.resolve(binding, power) { teamA }?.name)
        assertNull(state.resolve(binding, power) { teamB }, "in another team, the channel is gone")
        assertNull(state.resolve(binding, power) { null })
    }

    @Test
    fun Resolve_TeamChannel_ReachesTheSameNameInTheTeamTheOwnerIsInNow() {
        val state = ChannelState.EMPTY
            .create(alice, teamA, ChannelScope.TEAM, power, "base", ids)
            .create(alice, teamB, ChannelScope.TEAM, power, "base", ids)
        val binding = bind(alice, ChannelScope.TEAM, "base")
        assertEquals(teamB, state.resolve(binding, power) { teamB }?.owner)
        assertEquals(teamA, state.resolve(binding, power) { teamA }?.owner)
    }

    @Test
    fun Resolve_DeletedAndMadeAgain_JoinsAgainByName() {
        val first = ChannelState.EMPTY.create(alice, teamA, ChannelScope.PLAYER, power, "mine", ids)
        val binding = bind(alice, ChannelScope.PLAYER, "mine")
        val deleted = first.delete(alice, teamA, first.id("mine"))
        assertNull(deleted.resolve(binding, power) { teamA }, "nothing while it is gone")
        val again = deleted.create(alice, teamA, ChannelScope.PLAYER, power, "mine", ids)
        assertEquals(again.id("mine"), again.resolve(binding, power) { teamA }?.id, "a channel of that name takes its blocks back")
        assertNotEquals(first.id("mine"), again.id("mine"))
    }

    @Test
    fun Resolve_NamesMatchIgnoringCase_AndOnlyForTheResource() {
        val state = ChannelState.EMPTY.create(alice, teamA, ChannelScope.PLAYER, power, "Base", ids)
        assertEquals("Base", state.resolve(bind(alice, ChannelScope.PLAYER, "base"), power) { teamA }?.name)
        assertNull(state.resolve(bind(alice, ChannelScope.PLAYER, "base"), items) { teamA }, "another resource has no such channel")
    }

    @Test
    fun Resolve_NoBinding_Null() {
        assertNull(ChannelState.EMPTY.resolve(null, power) { teamA })
    }

    @Test
    fun WithoutTeamsNotIn_DropsTeamChannelsOfGoneTeams_KeepsPlayerChannels() {
        val state = ChannelState.EMPTY
            .create(alice, teamA, ChannelScope.TEAM, power, "a", ids)
            .create(alice, teamB, ChannelScope.TEAM, power, "b", ids)
            .create(alice, teamA, ChannelScope.PLAYER, power, "mine", ids)
        val kept = state.withoutTeamsNotIn(setOf(teamA))
        assertEquals(setOf("a", "mine"), kept.channels.values.map { it.name }.toSet())
        assertTrue(state.withoutTeamsNotIn(setOf(teamA, teamB)) === state)
    }

    @Test
    fun Of_DuplicateNamesAndWrongIds_Rejected() {
        val a = Channel(UUID(0, 1), power, ChannelScope.PLAYER, alice, "x")
        val b = Channel(UUID(0, 2), power, ChannelScope.PLAYER, alice, "X")
        assertThrows(IllegalArgumentException::class.java) { ChannelState.of(mapOf(a.id to a, b.id to b)) }
        assertThrows(IllegalArgumentException::class.java) { ChannelState.of(mapOf(UUID(0, 9) to a)) }
    }

    @Test
    fun Codec_ChannelAndBinding_RoundTrip() {
        val channel = Channel(UUID(1, 2), power, ChannelScope.TEAM, teamA, "base")
        assertEquals(channel, Channel.CODEC.parse(JsonOps.INSTANCE, Channel.CODEC.encodeStart(JsonOps.INSTANCE, channel).getOrThrow()).getOrThrow())
        val binding = ChannelBinding(alice, ChannelScope.TEAM, "base")
        assertEquals(binding, ChannelBinding.CODEC.parse(JsonOps.INSTANCE, ChannelBinding.CODEC.encodeStart(JsonOps.INSTANCE, binding).getOrThrow()).getOrThrow())
    }
}
