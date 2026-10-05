package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.channel.Channel
import com.itszuvalex.itszulib.channel.ChannelScope
import com.itszuvalex.itszulib.channel.ChannelUsers
import com.itszuvalex.itszulib.menu.MenuChannels
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ProblemReporter
import net.minecraft.world.level.storage.TagValueInput
import java.util.UUID

/**
 * Game tests for channels with the dev channel block: making a channel joins the block to it, other blocks join it by
 * its id and are counted, deleting leaves the blocks on it with no channel until one of that name is made again, a player
 * who leaves a team has no team channel until they are back, and a player cannot touch another's channels.
 */
object DevChannelGameTests {
    private val A = BlockPos(1, 1, 1)
    private val B = BlockPos(3, 1, 3)
    private val POWER = DevChannelBlockEntity.POWER
    private val ITEMS = DevChannelBlockEntity.ITEMS

    fun register(test: (String, Identifier, (GameTestHelper) -> Unit) -> Unit) {
        val area = DevGameTests.EMPTY_5X3X5
        test("channels_are_made_joined_and_counted", area, ::madeJoinedCounted)
        test("deleting_a_channel_leaves_blocks_waiting_and_a_new_one_of_the_name_takes_them_back", area, ::deleting)
        test("team_channels_follow_team_membership", area, ::teams)
        test("players_cannot_touch_each_others_channels", area, ::permissions)
        test("channel_bindings_are_saved_and_kept_while_their_channel_is_gone", area, ::saving)
        test("channel_menu_view_lists_what_the_player_can_see", area, ::view)
    }

    private fun player(helper: GameTestHelper): ServerPlayer {
        val player = helper.makeMockServerPlayerInLevel()
        ItszuLib.TEAMS.change { it.ensurePlayer(player.uuid, "channels_" + player.uuid.toString().take(6)) }
        return player
    }

    private fun block(helper: GameTestHelper, at: BlockPos): DevChannelBlockEntity {
        helper.setBlock(at, DevContent.DEV_CHANNEL_BLOCK.get())
        return helper.getBlockEntity(at, DevChannelBlockEntity::class.java)
    }

    private fun menu(player: ServerPlayer, be: DevChannelBlockEntity) = DevMenu(1, player.inventory, be).channels!!

    private fun named(player: UUID, name: String, resource: Identifier = POWER, scope: ChannelScope = ChannelScope.PLAYER): Channel? =
        ItszuLib.CHANNELS.visible(player, resource, scope).firstOrNull { it.name == name }

    private fun madeJoinedCounted(helper: GameTestHelper) {
        val player = player(helper)
        val first = block(helper, A)
        val second = block(helper, B)

        val menu = menu(player, first)
        helper.assertTrue(menu.handleText(MenuChannels.ACTION_CREATE, "Base"), "create handled")
        val channel = named(player.uuid, "Base")!!
        helper.assertTrue(first.channels.channel(POWER)?.id == channel.id, "making a channel joins the block to it")
        helper.assertTrue(first.channels.channel(ITEMS) == null, "other resources are untouched")
        helper.assertValueEqual(ChannelUsers.count(channel), 1, "one block on it")

        helper.assertTrue(menu(player, second).handleText(MenuChannels.ACTION_SELECT, channel.id.toString()), "select handled")
        helper.assertTrue(second.channels.channel(POWER)?.id == channel.id, "the second block joins by id")
        helper.assertValueEqual(ChannelUsers.count(channel), 2, "two blocks on it")
        helper.assertValueEqual(menu.compute().channels.single { it.id == channel.id }.count, 2, "the menu shows the count")

        menu.handleText(MenuChannels.ACTION_SELECT, "")
        helper.assertTrue(first.channels.channel(POWER) == null, "None leaves the channel")
        helper.assertValueEqual(ChannelUsers.count(channel), 1, "one block left on it")
        helper.succeed()
    }

    private fun deleting(helper: GameTestHelper) {
        val player = player(helper)
        val first = block(helper, A)
        val second = block(helper, B)
        val menu = menu(player, first)
        menu.handleText(MenuChannels.ACTION_CREATE, "Doomed")
        val doomed = named(player.uuid, "Doomed")!!
        menu(player, second).handleText(MenuChannels.ACTION_SELECT, doomed.id.toString())
        helper.assertValueEqual(ChannelUsers.count(doomed), 2, "both on it")

        menu.handleText(MenuChannels.ACTION_DELETE, doomed.id.toString())
        helper.assertTrue(named(player.uuid, "Doomed") == null, "the channel is gone")
        helper.assertTrue(first.channels.channel(POWER) == null && second.channels.channel(POWER) == null, "no block has it")
        helper.assertTrue(first.channels.binding(POWER)?.name == "Doomed", "but the blocks remember the name")
        helper.assertTrue(menu.compute().currentMissing && menu.compute().currentName == "Doomed", "and the menu says they are waiting for it")

        menu.handleText(MenuChannels.ACTION_CREATE, "Doomed")
        val again = named(player.uuid, "Doomed")!!
        helper.assertTrue(again.id != doomed.id, "a new channel")
        helper.assertTrue(second.channels.channel(POWER)?.id == again.id, "a block that was on the old one joins it again by name")
        helper.assertValueEqual(ChannelUsers.count(again), 2, "both blocks are on it again")
        helper.succeed()
    }

    private fun teams(helper: GameTestHelper) {
        val owner = player(helper)
        val member = player(helper)
        val teamId = ItszuLib.TEAMS.state.teamOf(owner.uuid)!!.id
        ItszuLib.TEAMS.change { it.invite(owner.uuid, member.uuid) }
        ItszuLib.TEAMS.change { it.accept(member.uuid, teamId) }
        helper.assertValueEqual(ItszuLib.TEAMS.state.teamOf(member.uuid)!!.id, teamId, "the member joined")

        val ownersBlock = block(helper, A)
        val membersBlock = block(helper, B)
        val ownerMenu = menu(owner, ownersBlock)
        val memberMenu = menu(member, membersBlock)
        ownerMenu.handle(MenuChannels.ACTION_SCOPE, ChannelScope.TEAM.ordinal)
        memberMenu.handle(MenuChannels.ACTION_SCOPE, ChannelScope.TEAM.ordinal)
        ownerMenu.handleText(MenuChannels.ACTION_CREATE, "Shared")
        val shared = named(owner.uuid, "Shared", scope = ChannelScope.TEAM)!!
        helper.assertTrue(memberMenu.compute().channels.any { it.id == shared.id }, "a team member sees it")
        memberMenu.handleText(MenuChannels.ACTION_SELECT, shared.id.toString())
        helper.assertValueEqual(ChannelUsers.count(shared), 2, "both members' blocks are on it")

        ItszuLib.TEAMS.change { it.leave(member.uuid) }
        helper.assertTrue(membersBlock.channels.channel(POWER) == null, "the player who left has no channel")
        helper.assertTrue(membersBlock.channels.binding(POWER)?.name == "Shared", "but the block remembers it")
        helper.assertTrue(ownersBlock.channels.channel(POWER)?.id == shared.id, "the team's own block keeps it")
        helper.assertTrue(memberMenu.compute().channels.none { it.id == shared.id }, "the player who left no longer sees it")

        ItszuLib.TEAMS.change { it.invite(owner.uuid, member.uuid) }
        ItszuLib.TEAMS.change { it.accept(member.uuid, ItszuLib.TEAMS.state.teamOf(owner.uuid)!!.id) }
        helper.assertTrue(membersBlock.channels.channel(POWER)?.id == shared.id, "back in the team, the member's block is on the channel again")
        ItszuLib.TEAMS.change { it.disband(owner.uuid) }
        helper.assertTrue(ItszuLib.CHANNELS.state[shared.id] == null, "a team's channels go with it")
        helper.assertTrue(ownersBlock.channels.channel(POWER) == null, "so does the block's channel")
        helper.succeed()
    }

    private fun permissions(helper: GameTestHelper) {
        val alice = player(helper)
        val bob = player(helper)
        val alicesBlock = block(helper, A)
        val bobsBlock = block(helper, B)
        menu(alice, alicesBlock).handleText(MenuChannels.ACTION_CREATE, "Private")
        val private = named(alice.uuid, "Private")!!
        val bobMenu = menu(bob, bobsBlock)

        bobMenu.handleText(MenuChannels.ACTION_SELECT, private.id.toString())
        helper.assertTrue(bobsBlock.channels.channel(POWER) == null, "bob cannot join alice's channel")
        helper.assertTrue(bobMenu.compute().message.isNotEmpty(), "and is told why: ${bobMenu.compute().message}")
        bobMenu.handleText(MenuChannels.ACTION_DELETE, private.id.toString())
        helper.assertTrue(ItszuLib.CHANNELS.state[private.id] != null, "bob cannot delete it")
        helper.assertTrue(alicesBlock.channels.channel(POWER) != null, "alice's block is still on it")

        bobMenu.handleText(MenuChannels.ACTION_CREATE, "")
        helper.assertTrue(bobMenu.compute().message.isNotEmpty(), "an empty name is refused")
        bobMenu.handleText(MenuChannels.ACTION_SELECT, "not an id")
        helper.assertTrue(bobMenu.compute().message.isNotEmpty(), "so is something that is not a channel")
        helper.succeed()
    }

    private fun saving(helper: GameTestHelper) {
        val player = player(helper)
        val be = block(helper, A)
        menu(player, be).handleText(MenuChannels.ACTION_CREATE, "Kept")
        val channel = named(player.uuid, "Kept")!!
        val registries = helper.level.registryAccess()
        val saved = be.saveWithFullMetadata(registries)

        be.channels.bind(POWER, player.uuid, null)
        helper.assertTrue(be.channels.channel(POWER) == null, "cleared")
        be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved))
        helper.assertTrue(be.channels.channel(POWER)?.id == channel.id, "the join is restored from the save")

        // The channel is deleted while the block is saved away (unloaded): it has no channel when it loads, and joins
        // again when a channel of that name is made.
        ItszuLib.CHANNELS.delete(player.uuid, channel.id)
        be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved))
        helper.assertTrue(be.channels.channel(POWER) == null, "a join to a deleted channel reaches nothing when loaded")
        helper.assertTrue(be.channels.binding(POWER)?.name == "Kept", "but is kept")
        ItszuLib.CHANNELS.create(player.uuid, ChannelScope.PLAYER, POWER, "kept")
        helper.assertTrue(be.channels.channel(POWER)?.name == "kept", "and joins a channel of that name made later, whatever its case")
        helper.succeed()
    }

    private fun view(helper: GameTestHelper) {
        val player = player(helper)
        val be = block(helper, A)
        val menu = menu(player, be)
        menu.handleText(MenuChannels.ACTION_CREATE, "b second")
        menu.handleText(MenuChannels.ACTION_CREATE, "A first")
        menu.handle(MenuChannels.ACTION_RESOURCE, 1)
        menu.handleText(MenuChannels.ACTION_CREATE, "items one")
        val items = menu.compute()
        helper.assertValueEqual(items.resources, listOf(POWER, ITEMS), "the block's resources")
        helper.assertTrue(items.resource == ITEMS, "looking at items")
        helper.assertValueEqual(items.channels.map { it.name }, listOf("items one"), "only the items channels")
        menu.handle(MenuChannels.ACTION_RESOURCE, 0)
        helper.assertValueEqual(menu.compute().channels.map { it.name }, listOf("A first", "b second"), "power channels by name")
        helper.assertValueEqual(menu.compute().currentName, "A first", "the block is on the last one it made")
        menu.handle(MenuChannels.ACTION_SCOPE, ChannelScope.TEAM.ordinal)
        helper.assertTrue(menu.compute().channels.isEmpty(), "no team channels yet")
        helper.assertValueEqual(menu.compute().currentName, "A first", "the current channel shows whichever scope is open")
        helper.assertTrue(!menu.handle(MenuChannels.ACTION_SCOPE, 9), "a scope that does not exist is refused")
        helper.assertTrue(be.getModule(Modules.CHANNELS, null) === be.channels, "the block offers its channels as a module")
        helper.succeed()
    }
}
