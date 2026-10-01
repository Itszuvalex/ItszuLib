package com.itszuvalex.itszulib.team

import com.mojang.serialization.DynamicOps
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.Identifier
import java.util.UUID

/**
 * Reads and writes [TeamState] as NBT.
 *
 * Decoding is strict: a missing field, a value of the wrong type, or a registered data type's value its codec rejects
 * throws [TeamDataFormatException] for the whole state. A partial read that quietly drops a team or a player's research
 * would be saved back over the good data, so it is never returned. Data of unregistered types is kept raw.
 *
 * Format (version 1):
 * ```
 * { version: 1, teams: [ { id: uuid, name, members: [ { player: uuid, role, name } ], invites: [uuid], data: { "<type id>": <value> } } ] }
 * ```
 */
object TeamCodec {
    const val VERSION = 1

    /**
     * @param ops Ops for data type codecs; pass registry-aware ops when a type's values reference registries.
     */
    @JvmStatic
    fun encode(state: TeamState, ops: DynamicOps<Tag>): CompoundTag {
        val root = CompoundTag()
        root.putInt("version", VERSION)
        val teams = ListTag()
        for (team in state.teams.values.sortedBy { it.id }) teams.add(encodeTeam(team, ops))
        root.put("teams", teams)
        return root
    }

    /**
     * @return The teams as saved. They are not yet checked against [TeamState]'s invariants; pass them to
     * [TeamState.repaired].
     * @throws TeamDataFormatException if the data cannot be read completely.
     */
    @JvmStatic
    fun decode(root: CompoundTag, ops: DynamicOps<Tag>): List<Team> {
        val version = root.getInt("version").orElseThrow { TeamDataFormatException("missing version") }
        if (version > VERSION) throw TeamDataFormatException("saved by a newer version ($version > $VERSION)")
        val teams = root.getList("teams").orElseThrow { TeamDataFormatException("missing teams") }
        return (0 until teams.size).map { i ->
            val tag = teams.getCompound(i).orElseThrow { TeamDataFormatException("teams[$i] is not a compound") }
            try {
                decodeTeam(tag, ops)
            } catch (e: TeamDataFormatException) {
                throw TeamDataFormatException("teams[$i]: ${e.message}")
            }
        }
    }

    internal fun encodeTeam(team: Team, ops: DynamicOps<Tag>): CompoundTag {
        val tag = CompoundTag()
        tag.putIntArray("id", UUIDUtil.uuidToIntArray(team.id))
        tag.putString("name", team.name)
        val members = ListTag()
        for ((player, member) in team.members.entries.sortedBy { it.key }) {
            val m = CompoundTag()
            m.putIntArray("player", UUIDUtil.uuidToIntArray(player))
            m.putString("role", member.role.name)
            m.putString("name", member.name)
            members.add(m)
        }
        tag.put("members", members)
        val invites = ListTag()
        for (player in team.invites.sorted()) invites.add(IntArrayTag(UUIDUtil.uuidToIntArray(player)))
        tag.put("invites", invites)
        val data = CompoundTag()
        for ((id, raw) in team.unknownData) data.put(id.toString(), raw.copy())
        for ((id, value) in team.data) {
            val type = TeamDataTypes.byId(id) ?: continue
            data.put(id.toString(), encodeValue(type, value, ops))
        }
        tag.put("data", data)
        return tag
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> encodeValue(type: TeamDataType<T>, value: Any, ops: DynamicOps<Tag>): Tag =
        type.codec.encodeStart(ops, value as T).getOrThrow { IllegalStateException("cannot encode ${type.id}: $it") }

    internal fun decodeTeam(tag: CompoundTag, ops: DynamicOps<Tag>): Team {
        val id = uuid(tag, "id")
        val name = tag.getString("name").orElseThrow { TeamDataFormatException("missing name") }
        val members = LinkedHashMap<UUID, TeamMember>()
        val memberList = tag.getList("members").orElseThrow { TeamDataFormatException("missing members") }
        for (i in 0 until memberList.size) {
            val m = memberList.getCompound(i).orElseThrow { TeamDataFormatException("members[$i] is not a compound") }
            val roleName = m.getString("role").orElseThrow { TeamDataFormatException("members[$i] missing role") }
            val role = TeamRole.entries.firstOrNull { it.name == roleName }
                ?: throw TeamDataFormatException("members[$i] has unknown role $roleName")
            val memberName = m.getString("name").orElseThrow { TeamDataFormatException("members[$i] missing name") }
            members[uuid(m, "player")] = TeamMember(role, memberName)
        }
        val inviteList = tag.getList("invites").orElseThrow { TeamDataFormatException("missing invites") }
        val invites = (0 until inviteList.size).map { i ->
            val raw = inviteList.get(i) as? IntArrayTag ?: throw TeamDataFormatException("invites[$i] is not a uuid")
            uuidOf(raw.getAsIntArray(), "invites[$i]")
        }.toSet()
        val dataTag = tag.getCompound("data").orElseThrow { TeamDataFormatException("missing data") }
        val data = HashMap<Identifier, Any>()
        val unknown = HashMap<Identifier, Tag>()
        for (key in dataTag.keySet()) {
            val typeId = Identifier.tryParse(key) ?: throw TeamDataFormatException("data has a bad type id $key")
            val raw = dataTag.get(key)!!
            val type = TeamDataTypes.byId(typeId)
            if (type == null) unknown[typeId] = raw.copy() else data[typeId] = decodeValue(type, raw, ops)
        }
        return Team(id, name, members, invites, data, unknown)
    }

    private fun <T : Any> decodeValue(type: TeamDataType<T>, raw: Tag, ops: DynamicOps<Tag>): T =
        type.codec.parse(ops, raw).result().orElseThrow {
            TeamDataFormatException("data ${type.id} could not be read: ${type.codec.parse(ops, raw).error().map { it.message() }.orElse("")}")
        }

    private fun uuid(tag: CompoundTag, key: String): UUID =
        uuidOf(tag.getIntArray(key).orElseThrow { TeamDataFormatException("missing $key") }, key)

    private fun uuidOf(ints: IntArray, where: String): UUID {
        if (ints.size != 4) throw TeamDataFormatException("$where is not a uuid")
        return UUIDUtil.uuidFromIntArray(ints)
    }
}

/**
 * Saved team data could not be read completely. Loading stops rather than continue with part of the data.
 */
class TeamDataFormatException(message: String) : RuntimeException(message)
