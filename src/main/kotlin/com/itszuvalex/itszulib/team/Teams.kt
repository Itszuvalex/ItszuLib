package com.itszuvalex.itszulib.team

import net.minecraft.nbt.Tag
import net.minecraft.resources.Identifier
import java.util.UUID

enum class TeamRole {
    /**
     * Made the team (or was handed it). Exactly one per team.
     */
    OWNER,

    /**
     * Promoted by the owner. Can invite players and remove any member except the owner.
     */
    OFFICER,

    MEMBER,
}

/**
 * @param name The player's last known name, kept so offline members can be shown and named.
 */
data class TeamMember(val role: TeamRole, val name: String)

/**
 * One team. Immutable: every change produces a new [Team] inside a new [TeamState].
 *
 * @param data Decoded values of registered [TeamDataType]s, keyed by type id. A missing entry reads as the type's
 * [TeamDataType.empty].
 * @param unknownData Saved data whose type is not registered (a removed type, or one from a mod that is not loaded).
 * Kept as raw tags and written back unchanged, so loading without that mod loses nothing.
 */
data class Team(
    val id: UUID,
    val name: String,
    val members: Map<UUID, TeamMember>,
    val invites: Set<UUID> = emptySet(),
    val data: Map<Identifier, Any> = emptyMap(),
    val unknownData: Map<Identifier, Tag> = emptyMap(),
) {
    val owner: UUID get() = members.entries.first { it.value.role == TeamRole.OWNER }.key

    fun roleOf(player: UUID): TeamRole? = members[player]?.role

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(type: TeamDataType<T>): T = data[type.id] as T? ?: type.empty()

    fun <T : Any> with(type: TeamDataType<T>, value: T): Team = copy(data = data + (type.id to value))

    /**
     * This team's data after [joining]'s data is merged in, as when a player from [joining] joins this team. Each
     * registered type uses its [TeamDataType.merge]; unknown raw data this team lacks is taken from [joining].
     */
    fun mergedWith(joining: Team): Team {
        var merged = data
        for (type in TeamDataTypes.all()) merged = merged + (type.id to mergeOne(type, joining))
        val unknown = joining.unknownData.filterKeys { it !in unknownData }.mapValues { it.value.copy() } + unknownData
        return copy(data = merged, unknownData = unknown)
    }

    private fun <T : Any> mergeOne(type: TeamDataType<T>, joining: Team): Any = type.merge(this[type], joining[type])

    /**
     * Copies of this team's data, as a leaving player takes them into their new solo team.
     */
    fun copiedData(): Pair<Map<Identifier, Any>, Map<Identifier, Tag>> {
        val copied = TeamDataTypes.all().associate { it.id to copyOne(it) }
        return copied to unknownData.mapValues { it.value.copy() }
    }

    private fun <T : Any> copyOne(type: TeamDataType<T>): Any = type.copy(this[type])
}

/**
 * A rule refused a team operation (e.g. a member tried to invite). Nothing was changed. [message] is shown to the
 * player who asked.
 */
class TeamException(message: String) : RuntimeException(message)

/**
 * Every team, and which team each player is in. Immutable: each operation returns a new state, built through [of],
 * which checks the invariants, so an operation either produces a valid state or changes nothing.
 *
 * Invariants (see [violations]):
 * - every player is in exactly one team;
 * - every team has exactly one owner and at least one member;
 * - no team invites its own members;
 * - every team has a non-blank name.
 *
 * Operations that create solo teams take a `newId` supplier for their ids (replaceable in tests).
 */
class TeamState private constructor(
    val teams: Map<UUID, Team>,
    private val playerTeams: Map<UUID, UUID>,
) {
    fun teamOf(player: UUID): Team? = playerTeams[player]?.let(teams::get)

    fun team(id: UUID): Team? = teams[id]

    /**
     * Teams that have invited [player].
     */
    fun invitesFor(player: UUID): List<Team> = teams.values.filter { player in it.invites }

    /**
     * Puts a new player in a new solo team they own, or refreshes a known player's stored name.
     */
    fun ensurePlayer(player: UUID, name: String, newId: () -> UUID = UUID::randomUUID): TeamState {
        val team = teamOf(player) ?: return with(soloTeam(newId(), player, name, emptyMap(), emptyMap()))
        val member = team.members.getValue(player)
        if (member.name == name) return this
        return with(team.copy(members = team.members + (player to member.copy(name = name))))
    }

    fun invite(actor: UUID, target: UUID): TeamState {
        val team = requireTeam(actor)
        requireManager(team, actor, "invite players")
        if (target in team.members) throw TeamException("That player is already in your team.")
        if (target in team.invites) throw TeamException("That player is already invited.")
        return with(team.copy(invites = team.invites + target))
    }

    fun revokeInvite(actor: UUID, target: UUID): TeamState {
        val team = requireTeam(actor)
        requireManager(team, actor, "revoke invites")
        if (target !in team.invites) throw TeamException("That player has no invite from your team.")
        return with(team.copy(invites = team.invites - target))
    }

    fun decline(player: UUID, teamId: UUID): TeamState {
        val team = teams[teamId]?.takeIf { player in it.invites } ?: throw TeamException("You have no invite from that team.")
        return with(team.copy(invites = team.invites - player))
    }

    /**
     * [player] joins [teamId], which must have invited them. Their current team's data is merged into the team
     * (their research before joining is unioned in). A solo team they leave behind is deleted; a shared one keeps its
     * data and other members.
     */
    fun accept(player: UUID, teamId: UUID): TeamState {
        val target = teams[teamId]?.takeIf { player in it.invites } ?: throw TeamException("You have no invite from that team.")
        val current = requireTeam(player)
        if (current.id == target.id) throw TeamException("You are already in that team.")
        requireCanLeave(current, player)
        val name = current.members.getValue(player).name
        val joined = target.mergedWith(current).let {
            it.copy(members = it.members + (player to TeamMember(TeamRole.MEMBER, name)), invites = it.invites - player)
        }
        return without(current, player).with(joined)
    }

    /**
     * [player] leaves their team for a new solo team holding a copy of its data.
     */
    fun leave(player: UUID, newId: () -> UUID = UUID::randomUUID): TeamState {
        val team = requireTeam(player)
        if (team.members.size == 1) throw TeamException("You are not in a team with anyone else.")
        requireCanLeave(team, player)
        return splitOff(team, player, newId())
    }

    /**
     * [actor] (owner or officer) removes [target], who keeps a copy of the team's data in a new solo team.
     */
    fun remove(actor: UUID, target: UUID, newId: () -> UUID = UUID::randomUUID): TeamState {
        val team = requireTeam(actor)
        requireManager(team, actor, "remove members")
        if (actor == target) throw TeamException("Leave the team instead of removing yourself.")
        val role = team.roleOf(target) ?: throw TeamException("That player is not in your team.")
        if (role == TeamRole.OWNER) throw TeamException("The owner cannot be removed.")
        return splitOff(team, target, newId())
    }

    fun promote(actor: UUID, target: UUID): TeamState = setRole(actor, target, TeamRole.MEMBER, TeamRole.OFFICER)

    fun demote(actor: UUID, target: UUID): TeamState = setRole(actor, target, TeamRole.OFFICER, TeamRole.MEMBER)

    /**
     * The owner hands the team to another member and becomes an officer.
     */
    fun transferOwnership(actor: UUID, target: UUID): TeamState {
        val team = requireTeam(actor)
        requireOwner(team, actor, "hand over the team")
        val member = team.members[target] ?: throw TeamException("That player is not in your team.")
        if (actor == target) throw TeamException("You already own the team.")
        val owner = team.members.getValue(actor)
        val members = team.members + (target to member.copy(role = TeamRole.OWNER)) + (actor to owner.copy(role = TeamRole.OFFICER))
        return with(team.copy(members = members))
    }

    fun rename(actor: UUID, name: String): TeamState {
        val team = requireTeam(actor)
        requireOwner(team, actor, "rename the team")
        if (name.isBlank()) throw TeamException("A team name cannot be blank.")
        return with(team.copy(name = name.trim()))
    }

    /**
     * The owner breaks up the team: every member, the owner included, gets a solo team with a copy of its data.
     */
    fun disband(actor: UUID, newId: () -> UUID = UUID::randomUUID): TeamState {
        val team = requireTeam(actor)
        requireOwner(team, actor, "disband the team")
        if (team.members.size == 1) throw TeamException("You are not in a team with anyone else.")
        val (data, unknown) = team.copiedData()
        var teams = this.teams - team.id
        for ((player, member) in team.members) {
            val solo = soloTeam(newId(), player, member.name, data, unknown)
            teams = teams + (solo.id to solo)
        }
        return of(teams.values)
    }

    /**
     * Replaces [teamId]'s value of [type] with `change(current value)`, e.g. to unlock research.
     */
    fun <T : Any> update(teamId: UUID, type: TeamDataType<T>, change: (T) -> T): TeamState {
        val team = teams[teamId] ?: throw TeamException("No such team.")
        return with(team.with(type, change(team[type])))
    }

    /**
     * Invariant violations, as human-readable lines; empty when the state is valid.
     */
    fun violations(): List<String> = violationsOf(teams.values)

    private fun setRole(actor: UUID, target: UUID, from: TeamRole, to: TeamRole): TeamState {
        val team = requireTeam(actor)
        requireOwner(team, actor, "change roles")
        val member = team.members[target] ?: throw TeamException("That player is not in your team.")
        if (member.role != from) throw TeamException("That player is not ${from.name.lowercase()}.")
        return with(team.copy(members = team.members + (target to member.copy(role = to))))
    }

    private fun splitOff(team: Team, player: UUID, soloId: UUID): TeamState {
        val (data, unknown) = team.copiedData()
        val solo = soloTeam(soloId, player, team.members.getValue(player).name, data, unknown)
        return without(team, player).with(solo)
    }

    private fun without(team: Team, player: UUID): TeamState =
        if (team.members.size == 1) of((teams - team.id).values)
        else with(team.copy(members = team.members - player))

    private fun with(team: Team): TeamState = of((teams + (team.id to team)).values)

    private fun requireTeam(player: UUID): Team = teamOf(player) ?: throw TeamException("You are not in a team.")

    private fun requireCanLeave(team: Team, player: UUID) {
        if (team.roleOf(player) == TeamRole.OWNER && team.members.size > 1) {
            throw TeamException("Hand the team to another member before leaving it.")
        }
    }

    private fun requireManager(team: Team, actor: UUID, what: String) {
        val role = team.roleOf(actor)
        if (role != TeamRole.OWNER && role != TeamRole.OFFICER) throw TeamException("Only the owner or an officer can $what.")
    }

    private fun requireOwner(team: Team, actor: UUID, what: String) {
        if (team.roleOf(actor) != TeamRole.OWNER) throw TeamException("Only the owner can $what.")
    }

    companion object {
        @JvmField
        val EMPTY = TeamState(emptyMap(), emptyMap())

        /**
         * Every player whose team differs between [old] and [new] (joined, left, removed, disbanded, or new), with
         * the team they were in (null for a new player) and the one they are in now, as each state has it.
         */
        @JvmStatic
        fun membershipChanges(old: TeamState, new: TeamState): List<MembershipChange> =
            new.playerTeams.entries.filter { (player, team) -> old.playerTeams[player] != team }
                .sortedBy { it.key }
                .map { (player, team) -> MembershipChange(player, old.teamOf(player), new.teams.getValue(team)) }

        /**
         * @throws IllegalStateException if [teams] break an invariant. Operations build their result through this, so
         * a bug in an operation fails it instead of producing an invalid state.
         */
        @JvmStatic
        fun of(teams: Collection<Team>): TeamState {
            val violations = violationsOf(teams)
            check(violations.isEmpty()) { "Invalid team state: ${violations.joinToString("; ")}" }
            return TeamState(teams.associateBy { it.id }, index(teams))
        }

        /**
         * Builds a valid state from possibly invalid [teams] (e.g. hand-edited or from an older version), repairing
         * deterministically rather than dropping teams wholesale. Each repair is reported to [report].
         * - A player in several teams stays in the first by team id.
         * - A team with no owner gets its first officer (else first member) by player id as owner; extra owners
         *   become officers.
         * - Invites to a team's own members are dropped; blank names become "Team".
         * - A team left with no members is dropped.
         */
        @JvmStatic
        fun repaired(teams: Collection<Team>, report: (String) -> Unit): TeamState {
            val seen = HashSet<UUID>()
            val fixed = ArrayList<Team>()
            for (team in teams.sortedBy { it.id }) {
                var members = team.members.filterKeys { player ->
                    seen.add(player).also { if (!it) report("Player $player was in several teams; removed from team ${team.id}") }
                }
                if (members.isEmpty()) {
                    report("Team ${team.id} has no members; dropped")
                    continue
                }
                val owners = members.filterValues { it.role == TeamRole.OWNER }.keys.sorted()
                if (owners.isEmpty()) {
                    val heir = members.entries.sortedWith(compareBy({ it.value.role != TeamRole.OFFICER }, { it.key })).first().key
                    report("Team ${team.id} has no owner; $heir is now owner")
                    members = members + (heir to members.getValue(heir).copy(role = TeamRole.OWNER))
                } else if (owners.size > 1) {
                    report("Team ${team.id} has ${owners.size} owners; keeping ${owners.first()}")
                    for (extra in owners.drop(1)) members = members + (extra to members.getValue(extra).copy(role = TeamRole.OFFICER))
                }
                val invites = team.invites - members.keys
                if (invites.size != team.invites.size) report("Team ${team.id} invited its own members; invites dropped")
                val name = team.name.ifBlank { "Team".also { report("Team ${team.id} had a blank name") } }
                fixed += team.copy(name = name, members = members, invites = invites)
            }
            return of(fixed)
        }

        private fun violationsOf(teams: Collection<Team>): List<String> {
            val problems = ArrayList<String>()
            val ids = HashSet<UUID>()
            val seen = HashMap<UUID, UUID>()
            for (team in teams) {
                if (!ids.add(team.id)) problems += "team ${team.id} appears twice"
                if (team.members.isEmpty()) problems += "team ${team.id} has no members"
                val owners = team.members.values.count { it.role == TeamRole.OWNER }
                if (team.members.isNotEmpty() && owners != 1) problems += "team ${team.id} has $owners owners"
                if (team.invites.any { it in team.members }) problems += "team ${team.id} invites its own members"
                if (team.name.isBlank()) problems += "team ${team.id} has a blank name"
                for (player in team.members.keys) {
                    seen.put(player, team.id)?.let { problems += "player $player is in teams $it and ${team.id}" }
                }
            }
            return problems
        }

        private fun index(teams: Collection<Team>): Map<UUID, UUID> =
            teams.flatMap { team -> team.members.keys.map { it to team.id } }.toMap()

        private fun soloTeam(id: UUID, player: UUID, name: String, data: Map<Identifier, Any>, unknown: Map<Identifier, Tag>) =
            Team(id, name, mapOf(player to TeamMember(TeamRole.OWNER, name)), emptySet(), data, unknown)
    }
}

/**
 * A player moving between teams: [from] as it was before the move (null for a new player), [to] as it is after.
 */
data class MembershipChange(val player: UUID, val from: Team?, val to: Team)
