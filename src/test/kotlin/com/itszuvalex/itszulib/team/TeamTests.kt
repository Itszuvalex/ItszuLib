package com.itszuvalex.itszulib.team

import net.minecraft.nbt.IntTag
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.StringTag
import net.minecraft.resources.Identifier
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

private val A = UUID(0, 1)
private val B = UUID(0, 2)
private val C = UUID(0, 3)
private val D = UUID(0, 4)
private val R1 = Identifier.fromNamespaceAndPath("itszulib_test", "one")
private val R2 = Identifier.fromNamespaceAndPath("itszulib_test", "two")
private val R3 = Identifier.fromNamespaceAndPath("itszulib_test", "three")

/**
 * Sequential team ids, so tests can name the teams they expect.
 */
private class Ids {
    private var next = 100L
    fun next(): UUID = UUID(1, next++)
}

private fun TeamState.research(player: UUID): Set<Identifier> = teamOf(player)!![Research.TYPE].unlocked

private fun TeamState.unlock(player: UUID, id: Identifier): TeamState = update(teamOf(player)!!.id, Research.TYPE) { it.unlock(id) }

private fun players(vararg names: Pair<UUID, String>, ids: Ids = Ids()): TeamState =
    names.fold(TeamState.EMPTY) { s, (p, n) -> s.ensurePlayer(p, n, ids::next) }

private fun assertRefused(state: TeamState, op: (TeamState) -> TeamState) {
    Assertions.assertThrows(TeamException::class.java) { op(state) }
}

/**
 * Joins [joiner] to [owner]'s team through an invite.
 */
private fun TeamState.join(owner: UUID, joiner: UUID): TeamState = invite(owner, joiner).accept(joiner, teamOf(owner)!!.id)

abstract class TeamTestBase {
    companion object {
        @BeforeAll
        @JvmStatic
        fun registerTypes() {
            TeamDataTypes.clear()
            TeamDataTypes.register(Research.TYPE)
        }

        @AfterAll
        @JvmStatic
        fun clearTypes() = TeamDataTypes.clear()
    }
}

class TeamStateTest : TeamTestBase() {
    @Test
    fun EnsurePlayer_NewPlayer_OwnsANewSoloTeam() {
        val state = players(A to "Ann")
        val team = state.teamOf(A)!!
        Assertions.assertEquals(mapOf(A to TeamMember(TeamRole.OWNER, "Ann")), team.members)
        Assertions.assertEquals("Ann", team.name)
        Assertions.assertTrue(state.research(A).isEmpty())
    }

    @Test
    fun EnsurePlayer_KnownPlayer_KeepsTeamAndUpdatesName() {
        val state = players(A to "Ann")
        val renamed = state.ensurePlayer(A, "Annie")
        Assertions.assertEquals(state.teamOf(A)!!.id, renamed.teamOf(A)!!.id)
        Assertions.assertEquals("Annie", renamed.teamOf(A)!!.members.getValue(A).name)
        Assertions.assertSame(renamed, renamed.ensurePlayer(A, "Annie"))
    }

    @Test
    fun Accept_UnionsTheJoiningPlayersResearchIntoTheTeam() {
        val state = players(A to "Ann", B to "Bo").unlock(A, R1).unlock(B, R2)
        val soloOfB = state.teamOf(B)!!.id
        val joined = state.join(A, B)
        Assertions.assertEquals(setOf(R1, R2), joined.research(A))
        Assertions.assertEquals(joined.teamOf(A), joined.teamOf(B))
        Assertions.assertEquals(TeamRole.MEMBER, joined.teamOf(B)!!.roleOf(B))
        Assertions.assertNull(joined.team(soloOfB), "B's emptied solo team is deleted")
        Assertions.assertTrue(B !in joined.teamOf(A)!!.invites)
    }

    @Test
    fun Accept_FromASharedTeam_OldTeamKeepsItsResearchAndOtherMembers() {
        val state = players(A to "Ann", B to "Bo", C to "Cy").unlock(B, R2).join(B, C)
        val teamOfB = state.teamOf(B)!!.id
        val moved = state.unlock(A, R1).invite(A, C).accept(C, state.teamOf(A)!!.id)
        Assertions.assertEquals(setOf(R1, R2), moved.research(C))
        Assertions.assertEquals(setOf(B), moved.team(teamOfB)!!.members.keys)
        Assertions.assertEquals(setOf(R2), moved.research(B))
    }

    @Test
    fun Accept_WithoutAnInvite_Refused() {
        val state = players(A to "Ann", B to "Bo")
        assertRefused(state) { it.accept(B, state.teamOf(A)!!.id) }
    }

    @Test
    fun Accept_OwnerOfASharedTeam_RefusedUntilOwnershipIsHandedOver() {
        val state = players(A to "Ann", B to "Bo", C to "Cy").join(B, C).invite(A, B)
        assertRefused(state) { it.accept(B, state.teamOf(A)!!.id) }
        val handed = state.transferOwnership(B, C).accept(B, state.teamOf(A)!!.id)
        Assertions.assertEquals(handed.teamOf(A), handed.teamOf(B))
        Assertions.assertEquals(TeamRole.OWNER, handed.teamOf(C)!!.roleOf(C))
    }

    @Test
    fun Decline_RemovesTheInviteOnly() {
        val state = players(A to "Ann", B to "Bo").invite(A, B)
        val declined = state.decline(B, state.teamOf(A)!!.id)
        Assertions.assertTrue(declined.teamOf(A)!!.invites.isEmpty())
        Assertions.assertNotEquals(declined.teamOf(A), declined.teamOf(B))
    }

    @Test
    fun Invite_ByMember_Refused_ByOfficer_Allowed() {
        val state = players(A to "Ann", B to "Bo", C to "Cy").join(A, B)
        assertRefused(state) { it.invite(B, C) }
        val promoted = state.promote(A, B)
        Assertions.assertTrue(C in promoted.invite(B, C).teamOf(A)!!.invites)
    }

    @Test
    fun Invite_ExistingMemberOrAlreadyInvited_Refused() {
        val state = players(A to "Ann", B to "Bo").join(A, B)
        assertRefused(state) { it.invite(A, B) }
        val c = state.ensurePlayer(C, "Cy").invite(A, C)
        assertRefused(c) { it.invite(A, C) }
    }

    @Test
    fun Leave_TakesACopyOfTheTeamsResearch_LaterUnlocksStaySeparate() {
        val state = players(A to "Ann", B to "Bo").unlock(A, R1).join(A, B).unlock(B, R2)
        val left = state.leave(B)
        Assertions.assertEquals(setOf(R1, R2), left.research(B))
        Assertions.assertNotEquals(left.teamOf(A)!!.id, left.teamOf(B)!!.id)
        Assertions.assertEquals(TeamRole.OWNER, left.teamOf(B)!!.roleOf(B))
        val later = left.unlock(A, R3)
        Assertions.assertEquals(setOf(R1, R2), later.research(B))
        Assertions.assertEquals(setOf(R1, R2, R3), later.research(A))
    }

    @Test
    fun Leave_SoloTeam_Refused() {
        assertRefused(players(A to "Ann")) { it.leave(A) }
    }

    @Test
    fun Leave_OwnerWithMembers_Refused() {
        assertRefused(players(A to "Ann", B to "Bo").join(A, B)) { it.leave(A) }
    }

    @Test
    fun Remove_OfficerRemovesAnotherOfficer_ButNotTheOwner() {
        val state = players(A to "Ann", B to "Bo", C to "Cy").join(A, B).join(A, C).promote(A, B).promote(A, C).unlock(A, R1)
        val removed = state.remove(B, C)
        Assertions.assertEquals(setOf(A, B), removed.teamOf(A)!!.members.keys)
        Assertions.assertEquals(setOf(R1), removed.research(C), "the removed player keeps a copy")
        assertRefused(state) { it.remove(B, A) }
    }

    @Test
    fun Remove_ByMember_Refused() {
        assertRefused(players(A to "Ann", B to "Bo", C to "Cy").join(A, B).join(A, C)) { it.remove(B, C) }
    }

    @Test
    fun PromoteAndDemote_OnlyTheOwner() {
        val state = players(A to "Ann", B to "Bo", C to "Cy").join(A, B).join(A, C).promote(A, B)
        assertRefused(state) { it.promote(B, C) }
        assertRefused(state) { it.demote(B, B) }
        Assertions.assertEquals(TeamRole.MEMBER, state.demote(A, B).teamOf(A)!!.roleOf(B))
    }

    @Test
    fun TransferOwnership_OldOwnerBecomesOfficer() {
        val state = players(A to "Ann", B to "Bo").join(A, B).transferOwnership(A, B)
        Assertions.assertEquals(B, state.teamOf(A)!!.owner)
        Assertions.assertEquals(TeamRole.OFFICER, state.teamOf(A)!!.roleOf(A))
    }

    @Test
    fun Disband_EveryMemberGetsASoloTeamWithACopy() {
        val ids = Ids()
        val state = players(A to "Ann", B to "Bo", C to "Cy", ids = ids).join(A, B).join(A, C).unlock(A, R1)
        val old = state.teamOf(A)!!.id
        val disbanded = state.disband(A, ids::next)
        Assertions.assertNull(disbanded.team(old))
        for (p in listOf(A, B, C)) {
            Assertions.assertEquals(setOf(R1), disbanded.research(p))
            Assertions.assertEquals(setOf(p), disbanded.teamOf(p)!!.members.keys)
        }
        assertRefused(state) { it.disband(B) }
    }

    @Test
    fun Rename_OwnerOnly_NotBlank() {
        val state = players(A to "Ann", B to "Bo").join(A, B)
        Assertions.assertEquals("Liches", state.rename(A, "  Liches ").teamOf(A)!!.name)
        assertRefused(state) { it.rename(B, "Mine") }
        assertRefused(state) { it.rename(A, "   ") }
    }

    @Test
    fun Of_InvalidState_Throws() {
        val twoOwners = Team(UUID(1, 1), "T", mapOf(A to TeamMember(TeamRole.OWNER, "a"), B to TeamMember(TeamRole.OWNER, "b")))
        Assertions.assertThrows(IllegalStateException::class.java) { TeamState.of(listOf(twoOwners)) }
        val shared = listOf(
            Team(UUID(1, 1), "T", mapOf(A to TeamMember(TeamRole.OWNER, "a"))),
            Team(UUID(1, 2), "U", mapOf(A to TeamMember(TeamRole.OWNER, "a"))),
        )
        Assertions.assertThrows(IllegalStateException::class.java) { TeamState.of(shared) }
    }

    @Test
    fun Repaired_FixesEachInvariantAndReportsIt() {
        val reports = ArrayList<String>()
        val repaired = TeamState.repaired(
            listOf(
                Team(UUID(1, 1), "T", mapOf(A to TeamMember(TeamRole.OWNER, "a"), B to TeamMember(TeamRole.MEMBER, "b"))),
                // B is also here; no owner; invites its own member; blank name.
                Team(UUID(1, 2), " ", mapOf(B to TeamMember(TeamRole.MEMBER, "b"), C to TeamMember(TeamRole.OFFICER, "c"), D to TeamMember(TeamRole.MEMBER, "d")), setOf(C)),
                Team(UUID(1, 3), "Empty", emptyMap()),
            ),
        ) { reports += it }
        Assertions.assertTrue(repaired.violations().isEmpty())
        Assertions.assertEquals(UUID(1, 1), repaired.teamOf(B)!!.id)
        val second = repaired.team(UUID(1, 2))!!
        Assertions.assertEquals(C, second.owner, "the officer inherits the team")
        Assertions.assertTrue(second.invites.isEmpty())
        Assertions.assertEquals("Team", second.name)
        Assertions.assertNull(repaired.team(UUID(1, 3)))
        Assertions.assertEquals(5, reports.size, reports.joinToString("\n"))
    }

    @Test
    fun Merge_UnknownData_TeamKeepsItsOwnAndGainsWhatItLacks() {
        val id1 = Identifier.fromNamespaceAndPath("gone", "a")
        val id2 = Identifier.fromNamespaceAndPath("gone", "b")
        val team = Team(UUID(1, 1), "T", mapOf(A to TeamMember(TeamRole.OWNER, "a")), unknownData = mapOf(id1 to StringTag.valueOf("team")))
        val joining = Team(UUID(1, 2), "U", mapOf(B to TeamMember(TeamRole.OWNER, "b")), unknownData = mapOf(id1 to StringTag.valueOf("joiner"), id2 to StringTag.valueOf("joiner")))
        val merged = team.mergedWith(joining)
        Assertions.assertEquals(StringTag.valueOf("team"), merged.unknownData[id1])
        Assertions.assertEquals(StringTag.valueOf("joiner"), merged.unknownData[id2])
    }
}

class TeamCodecTest : TeamTestBase() {
    private fun sample(): TeamState = players(A to "Ann", B to "Bo", C to "Cy").unlock(A, R1).join(A, B).promote(A, B).invite(A, C)

    @Test
    fun RoundTrip_KeepsEveryTeamExactly() {
        val state = sample()
        val decoded = TeamCodec.decode(TeamCodec.encode(state, NbtOps.INSTANCE), NbtOps.INSTANCE)
        Assertions.assertEquals(state.teams.values.toSet(), decoded.toSet())
    }

    @Test
    fun UnregisteredDataType_KeptRawAndWrittenBack() {
        val tag = TeamCodec.encode(sample(), NbtOps.INSTANCE)
        val teamTag = tag.getList("teams").get().getCompound(0).get()
        teamTag.getCompound("data").get().put("othermod:thing", StringTag.valueOf("kept"))
        val decoded = TeamCodec.decode(tag, NbtOps.INSTANCE)
        val again = TeamCodec.encode(TeamState.of(decoded), NbtOps.INSTANCE)
        val written = again.getList("teams").get().getCompound(0).get().getCompound("data").get()
        Assertions.assertEquals(StringTag.valueOf("kept"), written.get("othermod:thing"))
    }

    @Test
    fun RegisteredTypeWithUnreadableValue_FailsTheWholeDecode() {
        val tag = TeamCodec.encode(sample(), NbtOps.INSTANCE)
        tag.getList("teams").get().getCompound(0).get().getCompound("data").get().put(Research.TYPE.id.toString(), IntTag.valueOf(7))
        Assertions.assertThrows(TeamDataFormatException::class.java) { TeamCodec.decode(tag, NbtOps.INSTANCE) }
    }

    @Test
    fun MissingFieldOrNewerVersion_Fails() {
        val missing = TeamCodec.encode(sample(), NbtOps.INSTANCE)
        missing.getList("teams").get().getCompound(0).get().remove("members")
        Assertions.assertThrows(TeamDataFormatException::class.java) { TeamCodec.decode(missing, NbtOps.INSTANCE) }
        val newer = TeamCodec.encode(sample(), NbtOps.INSTANCE).also { it.putInt("version", TeamCodec.VERSION + 1) }
        Assertions.assertThrows(TeamDataFormatException::class.java) { TeamCodec.decode(newer, NbtOps.INSTANCE) }
    }
}

class TeamStoreTest : TeamTestBase() {
    @TempDir
    lateinit var dir: Path

    private val ops = NbtOps.INSTANCE
    private fun store() = TeamStore(dir.resolve("teams.dat"))
    private fun sample(): TeamState = players(A to "Ann", B to "Bo").unlock(A, R1).join(A, B)

    @Test
    fun NewWorld_EmptyAndWritable() {
        val store = store()
        Assertions.assertTrue(store.load(ops).teams.isEmpty())
        Assertions.assertTrue(store.writable)
    }

    /**
     * Stands in for a server restart: a fresh store reads what the last one wrote.
     */
    @Test
    fun SaveThenLoadInAFreshStore_RoundTrips() {
        val state = sample()
        store().also { it.load(ops) }.save(state, ops)
        Assertions.assertEquals(state.teams, store().load(ops).teams)
        Assertions.assertFalse(Files.exists(dir.resolve("teams.dat.tmp")))
    }

    @Test
    fun Save_KeepsThePreviousSaveAsBackup() {
        val store = store().also { it.load(ops) }
        val first = sample()
        store.save(first, ops)
        store.save(first.unlock(A, R2), ops)
        val backup = TeamCodec.decode(NbtIo.readCompressed(store.backup, net.minecraft.nbt.NbtAccounter.unlimitedHeap()), ops)
        Assertions.assertEquals(first.teams.values.toSet(), backup.toSet())
    }

    @Test
    fun UnreadableFileWithoutBackup_NotWritable_FileLeftUntouched() {
        val file = dir.resolve("teams.dat")
        Files.write(file, byteArrayOf(1, 2, 3, 4))
        val store = TeamStore(file)
        Assertions.assertTrue(store.load(ops).teams.isEmpty())
        Assertions.assertFalse(store.writable)
        Assertions.assertFalse(store.save(sample(), ops), "an empty or new state must never replace unreadable data")
        Assertions.assertArrayEquals(byteArrayOf(1, 2, 3, 4), Files.readAllBytes(file))
        // Still refuses next time, rather than treating the world as new.
        Assertions.assertFalse(TeamStore(file).also { it.load(ops) }.writable)
    }

    @Test
    fun PartlyUnreadableData_TreatedAsUnreadable_NotPartlyLoaded() {
        val file = dir.resolve("teams.dat")
        val tag = TeamCodec.encode(sample(), ops)
        tag.getList("teams").get().getCompound(0).get().getCompound("data").get().put(Research.TYPE.id.toString(), IntTag.valueOf(7))
        NbtIo.writeCompressed(tag, file)
        val store = TeamStore(file)
        Assertions.assertTrue(store.load(ops).teams.isEmpty())
        Assertions.assertFalse(store.writable)
    }

    @Test
    fun UnreadableFileWithGoodBackup_LoadsBackup_KeepsTheBadFileAside() {
        val store = store().also { it.load(ops) }
        val state = sample()
        store.save(state, ops)
        store.save(state, ops) // now teams.dat.bak holds a good copy
        Files.write(store.file, byteArrayOf(9, 9, 9))
        val reloaded = TeamStore(store.file)
        Assertions.assertEquals(state.teams, reloaded.load(ops).teams)
        Assertions.assertTrue(reloaded.writable)
        Assertions.assertTrue(Files.list(dir).use { files -> files.anyMatch { it.fileName.toString().startsWith("teams.dat.corrupt-") } })
    }

    @Test
    fun LeftoverTempFile_Ignored() {
        val store = store().also { it.load(ops) }
        store.save(sample(), ops)
        Files.write(dir.resolve("teams.dat.tmp"), byteArrayOf(0))
        Assertions.assertEquals(sample().teams, store().load(ops).teams)
    }
}

class TeamManagerTest : TeamTestBase() {
    @TempDir
    lateinit var dir: Path

    private fun loaded(): TeamManager = TeamManager().also { it.load(TeamStore(dir.resolve("teams.dat")), NbtOps.INSTANCE) }

    @Test
    fun RefusedChange_LeavesStateUnchangedAndNothingToSave() {
        val manager = loaded()
        manager.change { it.ensurePlayer(A, "Ann") }
        manager.save()
        val before = manager.state
        Assertions.assertThrows(TeamException::class.java) { manager.change { it.leave(A) } }
        Assertions.assertSame(before, manager.state)
        Files.delete(dir.resolve("teams.dat"))
        manager.save()
        Assertions.assertFalse(Files.exists(dir.resolve("teams.dat")), "nothing changed, so nothing is written")
    }

    @Test
    fun Change_ListenersSeeOldAndNew_AFailingListenerDoesNotUndoIt() {
        val manager = loaded()
        val seen = ArrayList<Pair<TeamState, TeamState>>()
        manager.onChange { _, _ -> throw IllegalStateException("listener failure") }
        manager.onChange { old, new -> seen += old to new }
        val result = manager.change { it.ensurePlayer(A, "Ann") }
        Assertions.assertSame(result, manager.state)
        Assertions.assertEquals(1, seen.size)
        Assertions.assertNull(seen.single().first.teamOf(A))
    }

    @Test
    fun Change_FromAnotherThread_Refused() {
        val manager = loaded()
        var error: Throwable? = null
        Thread { error = runCatching { manager.change { it.ensurePlayer(A, "Ann") } }.exceptionOrNull() }.apply { start(); join() }
        Assertions.assertInstanceOf(IllegalStateException::class.java, error)
        Assertions.assertNull(manager.state.teamOf(A))
    }

    @Test
    fun SaveAfterChange_ReachesDisk() {
        val manager = loaded()
        manager.change { it.ensurePlayer(A, "Ann").unlock(A, R1) }
        manager.save()
        Assertions.assertEquals(setOf(R1), TeamStore(dir.resolve("teams.dat")).load(NbtOps.INSTANCE).research(A))
    }

    @Test
    fun NotPersistent_WhenTheSavedDataIsUnreadable() {
        Files.write(dir.resolve("teams.dat"), byteArrayOf(1))
        val manager = loaded()
        Assertions.assertFalse(manager.isPersistent)
        manager.change { it.ensurePlayer(A, "Ann") }
        manager.save()
        Assertions.assertArrayEquals(byteArrayOf(1), Files.readAllBytes(dir.resolve("teams.dat")))
    }
}
