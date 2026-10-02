package com.itszuvalex.itszulib.team

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.research.TechTree
import com.itszuvalex.itszulib.research.TechnologyState
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.GameProfileArgument
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.NameAndId
import java.util.UUID

/**
 * `/itszulib team ...` for players, and `/itszulib research ...` for operators. A basic interface over
 * [TeamState]'s operations; refused requests report the rule's message and change nothing.
 */
object TeamCommands {
    private val ONE_PLAYER = SimpleCommandExceptionType(Component.literal("Name exactly one player."))

    @JvmStatic
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(ItszuLib.ID)
                .then(
                    Commands.literal("team")
                        .executes(::info)
                        .then(Commands.literal("info").executes(::info))
                        .then(Commands.literal("invite").then(profile("player").executes { invite(it) }))
                        .then(Commands.literal("revoke").then(profile("player").executes { onPlayer(it, "Invite revoked.") { s, a, t -> s.revokeInvite(a, t) } }))
                        .then(
                            Commands.literal("accept").executes { respond(it, null, accept = true) }
                                .then(Commands.argument("team", StringArgumentType.greedyString()).suggests(::invitingTeams).executes { respond(it, StringArgumentType.getString(it, "team"), accept = true) }),
                        )
                        .then(
                            Commands.literal("decline").executes { respond(it, null, accept = false) }
                                .then(Commands.argument("team", StringArgumentType.greedyString()).suggests(::invitingTeams).executes { respond(it, StringArgumentType.getString(it, "team"), accept = false) }),
                        )
                        .then(Commands.literal("leave").executes { change(it, "You left the team and kept a copy of its research.") { s, p -> s.leave(p.uuid) } })
                        .then(Commands.literal("remove").then(profile("player").executes { onPlayer(it, "Removed from the team.") { s, a, t -> s.remove(a, t) } }))
                        .then(Commands.literal("promote").then(profile("player").executes { onPlayer(it, "Promoted to officer.") { s, a, t -> s.promote(a, t) } }))
                        .then(Commands.literal("demote").then(profile("player").executes { onPlayer(it, "Demoted to member.") { s, a, t -> s.demote(a, t) } }))
                        .then(Commands.literal("transfer").then(profile("player").executes { onPlayer(it, "Ownership handed over.") { s, a, t -> s.transferOwnership(a, t) } }))
                        .then(
                            Commands.literal("rename").then(
                                Commands.argument("name", StringArgumentType.greedyString())
                                    .executes { ctx -> change(ctx, "Team renamed.") { s, p -> s.rename(p.uuid, StringArgumentType.getString(ctx, "name")) } },
                            ),
                        )
                        .then(Commands.literal("disband").executes { change(it, "Team disbanded; every member kept a copy of its research.") { s, p -> s.disband(p.uuid) } }),
                )
                .then(
                    Commands.literal("research")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(
                            Commands.literal("unlock").then(
                                Commands.argument("player", EntityArgument.player()).then(
                                    Commands.argument("research", IdentifierArgument.id()).suggests(::technologies).executes(::unlock),
                                ),
                            ),
                        )
                        .then(
                            Commands.literal("progress").then(
                                Commands.argument("player", EntityArgument.player()).then(
                                    Commands.argument("research", IdentifierArgument.id()).suggests(::technologies).then(
                                        Commands.argument("amount", LongArgumentType.longArg(1)).executes(::progress),
                                    ),
                                ),
                            ),
                        ),
                ),
        )
    }

    private fun profile(name: String) = Commands.argument(name, GameProfileArgument.gameProfile())

    private fun info(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val state = ItszuLib.TEAMS.state
        val team = state.teamOf(player.uuid) ?: return fail(ctx, "You are not in a team yet.")
        val lines = ArrayList<String>()
        lines += "Team ${team.name} (${team.members.size} member${if (team.members.size == 1) "" else "s"})"
        for ((_, member) in team.members.entries.sortedWith(compareBy({ it.value.role }, { it.value.name }))) {
            lines += "  ${member.name}: ${member.role.name.lowercase()}"
        }
        lines += "Research unlocked: ${team[Research.TYPE].unlocked.size}"
        if (team.invites.isNotEmpty()) lines += "Invited: ${team.invites.joinToString { nameOf(state, it) }}"
        val inviting = state.invitesFor(player.uuid)
        if (inviting.isNotEmpty()) lines += "Invites to you: ${inviting.joinToString { it.name }}"
        if (!ItszuLib.TEAMS.isPersistent) lines += "Warning: team data could not be loaded, so changes this session are not saved."
        ctx.source.sendSuccess({ Component.literal(lines.joinToString("\n")) }, false)
        return 1
    }

    private fun invite(ctx: CommandContext<CommandSourceStack>): Int {
        val target = onePlayer(ctx)
        val result = onPlayer(ctx, "Invited ${target.name()}.") { s, a, t -> s.invite(a, t) }
        if (result == 1) {
            val team = ItszuLib.TEAMS.state.teamOf(ctx.source.playerOrException.uuid)
            ctx.source.server.playerList.getPlayer(target.id())?.sendSystemMessage(
                Component.literal("You were invited to team ${team?.name}. Accept with /${ItszuLib.ID} team accept ${team?.name}"),
            )
        }
        return result
    }

    private fun respond(ctx: CommandContext<CommandSourceStack>, teamName: String?, accept: Boolean): Int {
        val player = ctx.source.playerOrException
        val invites = ItszuLib.TEAMS.state.invitesFor(player.uuid)
        val matching = if (teamName == null) invites else invites.filter { it.name.equals(teamName.trim(), ignoreCase = true) }
        val team = when {
            invites.isEmpty() -> return fail(ctx, "You have no invites.")
            matching.isEmpty() -> return fail(ctx, "No team named $teamName has invited you.")
            matching.size > 1 -> return fail(ctx, "Several teams invited you (${matching.joinToString { it.name }}); name one.")
            else -> matching.single()
        }
        return if (accept) {
            change(ctx, "You joined ${team.name}; your research is now shared with it.") { s, p -> s.accept(p.uuid, team.id) }
        } else {
            change(ctx, "Invite from ${team.name} declined.") { s, p -> s.decline(p.uuid, team.id) }
        }
    }

    private fun unlock(ctx: CommandContext<CommandSourceStack>): Int {
        val target = EntityArgument.getPlayer(ctx, "player")
        val id = IdentifierArgument.getId(ctx, "research")
        val team = ItszuLib.TEAMS.state.teamOf(target.uuid) ?: return fail(ctx, "That player is not in a team yet.")
        if (!TechTree.unlock(ctx.source.server, team.id, id)) return fail(ctx, "Team ${team.name} already has $id.")
        ctx.source.sendSuccess({ Component.literal("Unlocked $id for team ${team.name}.") }, true)
        return 1
    }

    private fun progress(ctx: CommandContext<CommandSourceStack>): Int {
        val target = EntityArgument.getPlayer(ctx, "player")
        val id = IdentifierArgument.getId(ctx, "research")
        val team = ItszuLib.TEAMS.state.teamOf(target.uuid) ?: return fail(ctx, "That player is not in a team yet.")
        val techs = TechTree.of(ctx.source.server.registryAccess())
        val state = techs.state(id, team[Research.TYPE]) ?: return fail(ctx, "No technology $id.")
        if (state != TechnologyState.AVAILABLE) return fail(ctx, "$id is ${state.name.lowercase()} for team ${team.name}.")
        val used = TechTree.addProgress(ctx.source.server, team.id, id, LongArgumentType.getLong(ctx, "amount"))
        val now = ItszuLib.TEAMS.state.team(team.id)?.get(Research.TYPE)
        val done = now?.has(id) == true
        ctx.source.sendSuccess({ Component.literal(if (done) "Added $used: $id is researched by team ${team.name}." else "Added $used: $id is at ${now?.progressOf(id)} / ${techs[id]?.cost}.") }, true)
        return 1
    }

    private fun technologies(ctx: CommandContext<CommandSourceStack>, builder: com.mojang.brigadier.suggestion.SuggestionsBuilder) =
        SharedSuggestionProvider.suggestResource(TechTree.of(ctx.source.registryAccess()).all.keys, builder)

    private fun onPlayer(ctx: CommandContext<CommandSourceStack>, message: String, op: (TeamState, UUID, UUID) -> TeamState): Int {
        val target = onePlayer(ctx).id()
        return change(ctx, message) { s, p -> op(s, p.uuid, target) }
    }

    private fun change(ctx: CommandContext<CommandSourceStack>, message: String, op: (TeamState, ServerPlayer) -> TeamState): Int {
        val player = ctx.source.playerOrException
        return try {
            ItszuLib.TEAMS.change { op(it, player) }
            ctx.source.sendSuccess({ Component.literal(message) }, false)
            1
        } catch (e: TeamException) {
            fail(ctx, e.message ?: "Refused.")
        }
    }

    private fun onePlayer(ctx: CommandContext<CommandSourceStack>): NameAndId =
        GameProfileArgument.getGameProfiles(ctx, "player").singleOrNull() ?: throw ONE_PLAYER.create()

    private fun nameOf(state: TeamState, player: UUID): String = state.teamOf(player)?.members?.get(player)?.name ?: player.toString()

    private fun invitingTeams(ctx: CommandContext<CommandSourceStack>, builder: com.mojang.brigadier.suggestion.SuggestionsBuilder) =
        SharedSuggestionProvider.suggest(
            ctx.source.player?.let { player -> ItszuLib.TEAMS.state.invitesFor(player.uuid).map { it.name } } ?: emptyList(),
            builder,
        )

    private fun fail(ctx: CommandContext<CommandSourceStack>, message: String): Int {
        ctx.source.sendFailure(Component.literal(message))
        return 0
    }
}
