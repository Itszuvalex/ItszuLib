package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.research.TechTree
import com.itszuvalex.itszulib.research.TechnologyResearchedEvent
import com.itszuvalex.itszulib.research.TechnologyState
import com.itszuvalex.itszulib.team.Research
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.neoforged.neoforge.common.NeoForge
import java.util.UUID

/**
 * Tech tree tests over the dev technologies (`data/itszulib/itszulib/technology/dev_*.json`, tree `itszulib:dev`,
 * loaded only in dev through the `itszulib:dev_environment` condition). Each test's mock player has a solo team of
 * its own, so tests do not share research.
 */
object DevResearchGameTests {
    private val TREE = dev("dev")
    private val ROOT = dev("dev_root")
    private val BRANCH = dev("dev_branch")
    private val SECRET = dev("dev_secret")
    private val FAR = dev("dev_far")
    private val FREE = dev("dev_free")
    private val REQUIREMENTS = dev("dev_requirements")
    private val DEV_POWER = dev("dev_power")

    private val researched = ArrayList<Pair<UUID, Identifier>>()
    private val moves = ArrayList<com.itszuvalex.itszulib.team.MembershipChange>()

    private fun dev(path: String) = Identifier.fromNamespaceAndPath(ItszuLib.ID, path)

    fun register(test: (String, (GameTestHelper) -> Unit) -> Unit) {
        NeoForge.EVENT_BUS.addListener { e: TechnologyResearchedEvent -> researched += e.team to e.technology }
        NeoForge.EVENT_BUS.addListener { e: com.itszuvalex.itszulib.team.TeamMembershipChangedEvent -> moves += e.change }
        test("tech_tree_loads_datapack_technologies", ::loads)
        test("tech_tree_progress_unlocks_and_posts_event", ::progressUnlocks)
        test("tech_tree_states_follow_team_research", ::states)
        test("tech_tree_queue_focus_follows_research", ::queue)
        test("team_moves_post_membership_events", ::membership)
        test("tech_tree_resources_items_and_rewards", ::requirements)
    }

    private fun teamOf(helper: GameTestHelper): Pair<Player, UUID> {
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        ItszuLib.TEAMS.change { it.ensurePlayer(player.uuid, "research_test") }
        return player to ItszuLib.TEAMS.state.teamOf(player.uuid)!!.id
    }

    private fun loads(helper: GameTestHelper) {
        val techs = TechTree.of(helper.level.registryAccess())
        helper.assertValueEqual(techs.inTree(TREE).keys, setOf(ROOT, BRANCH, SECRET, FAR, FREE, REQUIREMENTS), "dev tree")
        helper.assertTrue(TREE in techs.trees, "trees")
        helper.assertValueEqual(techs.problems(), emptyList<String>(), "problems")
        val branch = techs[BRANCH]!!
        helper.assertValueEqual(branch.cost, 50L, "cost")
        helper.assertValueEqual(branch.prerequisites, listOf(ROOT), "prerequisites")
        helper.assertTrue(branch.icon === Items.IRON_INGOT, "icon")
        helper.assertValueEqual(techs[FREE]!!.displayName(FREE).string, "Dev free technology", "name override")
        helper.assertValueEqual(techs[ROOT]!!.displayName(ROOT).contents.toString().contains("technology.itszulib.dev_root"), true, "default name key")
        val layout = techs.layout(TREE)
        helper.assertValueEqual(layout.positions.size, 6, "laid out")
        val requirements = techs[REQUIREMENTS]!!
        helper.assertValueEqual(requirements.resources, mapOf(DEV_POWER to 100L), "resources")
        helper.assertTrue(requirements.items.single().count() == 4 && requirements.items.single().ingredient().test(net.minecraft.world.item.ItemStack(Items.IRON_INGOT)), "items")
        helper.assertTrue(requirements.rewards.single().item().value() === Items.DIAMOND, "rewards")
        helper.assertTrue(layout.edges.any { it.from == ROOT && it.to == FAR && it.waypoints.isNotEmpty() }, "skip link has waypoints")
        helper.succeed()
    }

    private fun progressUnlocks(helper: GameTestHelper) {
        val (_, team) = teamOf(helper)
        val server = helper.level.server
        helper.assertValueEqual(TechTree.addProgress(server, team, BRANCH, 10), 0L, "locked takes nothing")
        helper.assertValueEqual(TechTree.addProgress(server, team, ROOT, 60), 60L, "first batch")
        helper.assertValueEqual(ItszuLib.TEAMS.state.team(team)!![Research.TYPE].progressOf(ROOT), 60L, "progress stored")
        helper.assertValueEqual(TechTree.addProgress(server, team, ROOT, 100), 40L, "only what was needed")
        val research = ItszuLib.TEAMS.state.team(team)!![Research.TYPE]
        helper.assertTrue(research.has(ROOT), "unlocked at cost")
        helper.assertValueEqual(research.progressOf(ROOT), 0L, "progress cleared")
        helper.assertTrue(researched.contains(team to ROOT), "event posted")
        helper.assertValueEqual(TechTree.addProgress(server, team, ROOT, 5), 0L, "researched takes nothing")
        helper.assertTrue(TechTree.unlock(server, team, SECRET), "forced unlock")
        helper.assertFalse(TechTree.unlock(server, team, SECRET), "already unlocked")
        // dev_far costs 0: offering nothing researches it once it is available.
        helper.assertValueEqual(TechTree.addProgress(server, team, FAR, 0), 0L, "free technology")
        helper.assertTrue(ItszuLib.TEAMS.state.team(team)!![Research.TYPE].has(FAR), "free technology unlocked")
        helper.succeed()
    }

    /**
     * A technology that also needs a resource and items unlocks only once all are in; its rewards go to the member
     * online once, and to a player joining the team later.
     */
    private fun requirements(helper: GameTestHelper) {
        val player = helper.makeMockServerPlayerInLevel()
        ItszuLib.TEAMS.change { it.ensurePlayer(player.uuid, "research_test") }
        val team = ItszuLib.TEAMS.state.teamOf(player.uuid)!!.id
        val server = helper.level.server
        fun research() = ItszuLib.TEAMS.state.team(team)!![Research.TYPE]
        TechTree.unlock(server, team, ROOT)
        helper.assertValueEqual(TechTree.addProgress(server, team, REQUIREMENTS, 50), 10L, "points up to the cost")
        helper.assertFalse(research().has(REQUIREMENTS), "points alone do not unlock")
        helper.assertValueEqual(TechTree.addResource(server, team, REQUIREMENTS, dev("other"), 10), 0L, "a resource it does not need")
        helper.assertValueEqual(TechTree.addResource(server, team, REQUIREMENTS, DEV_POWER, 150), 100L, "resource up to its amount")
        val stacks = listOf(net.minecraft.world.item.ItemStack(Items.IRON_INGOT, 3), net.minecraft.world.item.ItemStack(Items.GOLD_INGOT, 5), net.minecraft.world.item.ItemStack(Items.IRON_INGOT, 5))
        helper.assertValueEqual(TechTree.deliver(server, team, REQUIREMENTS, stacks), 4, "items taken")
        helper.assertValueEqual(stacks.map { it.count }, listOf(0, 5, 4), "matching stacks shrunk")
        helper.assertTrue(research().has(REQUIREMENTS), "unlocked once everything is in")
        helper.assertTrue(research().requirements.isEmpty(), "requirement progress cleared")
        helper.assertValueEqual(player.inventory.countItem(Items.DIAMOND), 1, "reward given")
        helper.assertTrue(research().hasClaimed(REQUIREMENTS, player.uuid), "claim recorded")
        helper.assertValueEqual(TechTree.claimRewards(player), 0, "only once")

        val joiner = helper.makeMockServerPlayerInLevel()
        ItszuLib.TEAMS.change { it.ensurePlayer(joiner.uuid, "research_joiner") }
        ItszuLib.TEAMS.change { it.invite(player.uuid, joiner.uuid).accept(joiner.uuid, team) }
        helper.assertValueEqual(joiner.inventory.countItem(Items.DIAMOND), 1, "a joiner gets the team's rewards")
        helper.succeed()
    }

    private fun queue(helper: GameTestHelper) {
        val (_, team) = teamOf(helper)
        val server = helper.level.server
        fun queue() = ItszuLib.TEAMS.state.team(team)!![Research.TYPE].queue
        helper.assertTrue(TechTree.queue(server, team, FAR), "queued with its prerequisites")
        helper.assertValueEqual(queue(), listOf(ROOT, BRANCH, SECRET, FAR), "path in order")
        helper.assertFalse(TechTree.queue(server, team, BRANCH), "already queued")
        helper.assertTrue(TechTree.focus(server, team, TREE) == ROOT, "focus is the first available")
        TechTree.addProgress(server, team, ROOT, 100)
        helper.assertValueEqual(queue(), listOf(BRANCH, SECRET, FAR), "researched leaves the queue")
        helper.assertTrue(TechTree.focus(server, team, TREE) == BRANCH, "focus moves on")
        helper.assertTrue(TechTree.unqueue(server, team, BRANCH), "unqueued")
        helper.assertValueEqual(queue(), emptyList<Identifier>(), "dependents go with it")
        helper.assertTrue(TechTree.focus(server, team, TREE) == null, "no focus")
        helper.succeed()
    }

    private fun membership(helper: GameTestHelper) {
        val (owner, team) = teamOf(helper)
        val (joiner, solo) = teamOf(helper)
        helper.assertTrue(moves.any { it.player == joiner.uuid && it.from == null && it.to.id == solo }, "new player's solo team")
        TechTree.queue(helper.level.server, solo, BRANCH)
        moves.removeIf { it.player == joiner.uuid }
        ItszuLib.TEAMS.change { it.invite(owner.uuid, joiner.uuid).accept(joiner.uuid, team) }
        fun mine() = moves.filter { it.player == joiner.uuid }
        val join = mine().singleOrNull()
        helper.assertTrue(join != null && join.player == joiner.uuid && join.from?.id == solo && join.to.id == team, "join posted once, got $moves")
        helper.assertValueEqual(join!!.to[Research.TYPE].queue, listOf(ROOT, BRANCH), "data already merged")
        moves.removeIf { it.player == joiner.uuid }
        ItszuLib.TEAMS.change { it.leave(joiner.uuid) }
        val leave = mine().singleOrNull()
        helper.assertTrue(leave != null && leave.from?.id == team && leave.to.id != team, "leave posted once, got $moves")
        helper.assertValueEqual(leave!!.to[Research.TYPE].queue, listOf(ROOT, BRANCH), "leaver keeps a copy")
        helper.succeed()
    }

    private fun states(helper: GameTestHelper) {
        val (player, team) = teamOf(helper)
        val techs = TechTree.of(helper.level.registryAccess())
        fun state(id: Identifier): TechnologyState = techs.state(id, TechTree.research(player)) ?: TechnologyState.HIDDEN
        helper.assertTrue(TechTree.isResearched(player, FREE), "unlocked by default")
        helper.assertValueEqual(state(ROOT), TechnologyState.AVAILABLE, "root")
        helper.assertValueEqual(state(BRANCH), TechnologyState.LOCKED, "branch")
        helper.assertValueEqual(state(SECRET), TechnologyState.HIDDEN, "secret")
        TechTree.unlock(helper.level.server, team, ROOT)
        TechTree.unlock(helper.level.server, team, BRANCH)
        helper.assertTrue(TechTree.isResearched(player, BRANCH), "branch researched")
        helper.assertValueEqual(state(SECRET), TechnologyState.AVAILABLE, "secret revealed")
        helper.assertValueEqual(state(FAR), TechnologyState.LOCKED, "far still needs secret")
        helper.succeed()
    }
}
