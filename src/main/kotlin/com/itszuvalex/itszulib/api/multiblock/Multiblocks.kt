package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.storage.IFluidStorage
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.utility.ChunkCoord
import com.itszuvalex.itszulib.api.utility.DirectionUtil
import com.itszuvalex.itszulib.api.utility.Loc4
import com.itszuvalex.itszulib.core.EnumAutomaticIO
import com.itszuvalex.itszulib.core.SidedFluidStorageConfiguration
import com.itszuvalex.itszulib.core.SidedItemStorageConfiguration
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.common.util.ValueIOSerializable
import org.jetbrains.annotations.TestOnly
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * State shared by a whole formed structure (inventories, progress). It lives on the structure's home member (the one
 * at offset (0,0,0), see [MultiblockShape]) and is saved with that member's chunk, so nothing holds it while the
 * structure is unloaded. Other members reach it through [MultiblockManager]; while any member is loaded the manager
 * keeps the home member's chunk loaded (see [MultiblockManager]).
 */
interface IMultiblockState : ValueIOSerializable {
    /**
     * Server side, once, when the structure breaks because a member was removed: drop contents here. [anchor] is the
     * home member's position and [brokenAt] the removed member's. Not called by [MultiblockManager.disband].
     */
    fun onBreak(level: ILevel, anchor: BlockPos, brokenAt: BlockPos) {}
}

/**
 * A declarative multiblock pattern: relative-position slots, each requiring a named role. Port of TechnoLich's
 * `MultiblockShape`, with shared state: there is no controller block, but the slot at (0,0,0) is required and is the
 * structure's home, which holds its [IMultiblockState] if [stateFactory] is set.
 *
 * A role may occupy more than one slot; any part that can fill that role may occupy any of them. Matching is
 * axis-aligned and fixed-orientation only; a rotated variant needs its own registered shape.
 *
 * [breakPolicy] decides what breaking one member does to the others.
 */
class MultiblockShape private constructor(
    val id: Identifier,
    slots: Map<BlockPos, String>,
    val breakPolicy: MultiblockBreakPolicy,
    val stateFactory: ((onChanged: Runnable) -> IMultiblockState)?,
) {
    val slots: Map<BlockPos, String> = slots.toMap()

    /**
     * Offsets that may satisfy each role.
     */
    val offsetsByRole: Map<String, List<BlockPos>> = this.slots.entries.groupBy({ it.value }, { it.key })

    val hasState: Boolean get() = stateFactory != null

    /**
     * World positions of every slot for a structure anchored at [anchor].
     */
    fun positions(anchor: BlockPos): List<BlockPos> = slots.keys.map { anchor.offset(it) }

    override fun toString(): String = "MultiblockShape[$id]"

    companion object {
        private val SHAPES = ConcurrentHashMap<Identifier, MultiblockShape>()

        /**
         * @param state Creates the structure's shared state on its home member; the runnable marks that member dirty.
         * @throws IllegalArgumentException if [slots] has no (0,0,0) slot, or a shape with this id is already
         * registered.
         */
        @JvmStatic
        @JvmOverloads
        fun register(
            id: Identifier,
            slots: Map<BlockPos, String>,
            breakPolicy: MultiblockBreakPolicy = MultiblockBreakPolicy.DISSOLVE,
            state: ((onChanged: Runnable) -> IMultiblockState)? = null,
        ): MultiblockShape {
            require(BlockPos.ZERO in slots) { "Shape $id has no slot at (0,0,0), its home" }
            val shape = MultiblockShape(id, slots, breakPolicy, state)
            require(SHAPES.putIfAbsent(id, shape) == null) { "Shape with id: $id already registered." }
            return shape
        }

        /**
         * Slots of an [x] by [y] by [z] box of [role], with (0,0,0) at its lowest corner.
         */
        @JvmStatic
        fun box(x: Int, y: Int, z: Int, role: String): Map<BlockPos, String> =
            (0 until x).flatMap { dx -> (0 until y).flatMap { dy -> (0 until z).map { dz -> BlockPos(dx, dy, dz) to role } } }.toMap()

        @JvmStatic
        fun byId(id: Identifier): MultiblockShape? = SHAPES[id]

        @TestOnly
        @JvmStatic
        fun clear() = SHAPES.clear()
    }
}

/**
 * What breaking one member of a formed structure does to the rest.
 */
enum class MultiblockBreakPolicy {
    /**
     * The structure dissolves: every other member stays in the world and leaves the structure, free to form again.
     * Members in unloaded chunks find out when they next load (see [MultiblockManager]).
     */
    DISSOLVE,

    /**
     * Every other member is destroyed too (as if broken, with drops). Siblings are looked up through the level, which
     * loads their chunks, so no orphaned pieces are left.
     */
    DESTROY_ALL,
}

/**
 * One role a [IMultiblockMember] can fill, in one [shape].
 */
data class MultiblockRoleRef(val shape: MultiblockShape, val role: String)

/**
 * A formed structure's identity plus which slot one member occupies. [structureId] is minted once, when the structure
 * forms, and is stable until it breaks.
 */
data class MultiblockMembership(val structureId: UUID, val shape: MultiblockShape, val offset: BlockPos) {
    val isHome: Boolean get() = offset == BlockPos.ZERO
}

/**
 * Something that can be a multiblock member. [com.itszuvalex.itszulib.core.frag.FragMultiblockPart] is the
 * fragment-based implementation; anything else that persists its own [membership] (and, as home, its [state]) and
 * calls [MultiblockManager]'s `onPartLoaded`/`onPartUnloaded`/`onPartRemoved` at the right lifecycle points can take
 * part too. Members of one structure need not share a block, a block entity type, or a mod.
 */
interface IMultiblockMember {
    val candidateRoles: List<MultiblockRoleRef>

    /**
     * Whether [MultiblockManager.onPartLoaded] tries to form a structure from this member's [candidateRoles]. Members
     * that only join through [MultiblockManager.form] return false.
     */
    val autoForm: Boolean get() = true

    val membership: MultiblockMembership?

    /**
     * The shared state this member holds: non-null only on a stateful structure's home member (server side).
     */
    val state: IMultiblockState? get() = null

    /**
     * Called by [MultiblockManager], once, when this member's slot joins a newly formed structure. Implementations
     * persist [membership], create the shape's state if they are the home member, and mark themselves dirty.
     */
    fun join(membership: MultiblockMembership)

    /**
     * Called by [MultiblockManager] when this member's structure is gone. Implementations clear their membership and
     * state and mark themselves dirty.
     */
    fun leave()
}

/**
 * Loads a chunk without ticking it for a short while; replaceable for tests.
 */
interface IChunkTickets {
    /**
     * Adds the ticket on [chunk], or restarts its timeout if it is already there.
     */
    fun refresh(level: ILevel, chunk: ChunkCoord)

    /**
     * Whether blocks tick at [pos]: true for chunks a player or a ticking ticket keeps loaded, false for chunks only
     * our own (non-ticking) ticket loads.
     */
    fun isTicking(level: ILevel, pos: BlockPos): Boolean
}

/**
 * One formed structure, in memory only and rebuilt as members load: members persist their own [MultiblockMembership]
 * (see [com.itszuvalex.itszulib.core.frag.FragMultiblockPart]), so an instance with members not yet loaded is normal.
 */
class MultiblockInstance(val id: UUID, val shape: MultiblockShape, val level: ILevel, val anchorPos: BlockPos) {
    private val members = HashMap<BlockPos, IMultiblockMember>()

    private var lastTick = Long.MIN_VALUE

    val anchor: Loc4 get() = Loc4.of(level, anchorPos)

    val homeChunk: ChunkCoord get() = ChunkCoord.of(anchorPos)

    /**
     * @return The member at [offset] (relative to the anchor), if loaded and registered.
     */
    fun memberAt(offset: BlockPos): IMultiblockMember? = members[offset]

    fun posFor(offset: BlockPos): BlockPos = anchorPos.offset(offset)

    /**
     * The home member, if loaded.
     */
    val home: IMultiblockMember? get() = members[BlockPos.ZERO]

    /**
     * The shared state, if the home member is loaded.
     */
    val state: IMultiblockState? get() = home?.state

    /**
     * Every loaded member, keyed by offset. Short of [MultiblockShape.slots] whenever part of the structure is in an
     * unloaded chunk.
     */
    fun loadedMembers(): Map<BlockPos, IMultiblockMember> = members

    /**
     * @return True the first time it is called for [gameTime]: lets every member tick the structure, and the first to
     * do so in a tick do it for all.
     */
    fun claimTick(gameTime: Long): Boolean {
        if (lastTick == gameTime) return false
        lastTick = gameTime
        return true
    }

    internal fun isEmpty(): Boolean = members.isEmpty()

    internal fun register(offset: BlockPos, member: IMultiblockMember) {
        members[offset] = member
    }

    internal fun unregister(offset: BlockPos) {
        members.remove(offset)
    }
}

/**
 * Tracks formed multiblock structures; server only ([SERVER]). Port of TechnoLich's `MultiblockManager`, with shared
 * state, explicit formation and verification.
 *
 * Formation: a member whose [IMultiblockMember.autoForm] is true tries, when it loads without a membership, every
 * shape and offset its [IMultiblockMember.candidateRoles] allow ([onPartLoaded]); [form] forms a given shape at a given
 * anchor. Either needs every slot loaded at that moment, and never loads a chunk to look.
 *
 * Shared state lives on the home member (offset (0,0,0)), saved with its chunk, so nothing keeps an unloaded
 * structure's data in memory. While a stateful structure has a member outside the home member's chunk whose own chunk
 * ticks, [tick] keeps a ticket on the home chunk that loads it without ticking it (radius 0, [TICKET_TIMEOUT] ticks,
 * refreshed every [TICKET_REFRESH] ticks). So the state is reachable from every member that does anything, and when
 * those members stop ticking (the player left) the ticket lapses and the home chunk unloads normally. A member in a
 * chunk that only this ticket loads never refreshes one, so structures cannot keep each other loaded. A member in
 * another chunk can see a null state for the moment the home chunk takes to load.
 *
 * Verification: a member that loads remembering a structure is checked against the home member once the home position
 * is loaded ([verifyPending], run every server tick). If the home member no longer belongs to that structure (it broke
 * or was disbanded while this member was unloaded), the structure's loaded members leave. So no record of structures
 * is kept beyond the members themselves.
 *
 * @param destroyBlock Removes a sibling's block for [MultiblockBreakPolicy.DESTROY_ALL] (as if broken, with drops).
 */
class MultiblockManager(
    private val destroyBlock: (ILevel, BlockPos) -> Unit = { level, pos -> level.toMinecraft().destroyBlock(pos, true) },
    private val tickets: IChunkTickets = VanillaChunkTickets,
) {
    private val instances = HashMap<UUID, MultiblockInstance>()

    /**
     * Structures being torn down by [onPartRemoved], so the removals it causes don't recurse.
     */
    private val breaking = HashSet<UUID>()

    private val unverified = LinkedHashSet<UUID>()

    fun get(id: UUID): MultiblockInstance? = instances[id]

    /**
     * The loaded structure [member] belongs to.
     */
    fun instanceOf(member: IMultiblockMember): MultiblockInstance? = member.membership?.let { instances[it.structureId] }

    /**
     * The shared state of [member]'s structure, if its home member is loaded.
     */
    fun stateOf(member: IMultiblockMember): IMultiblockState? {
        val m = member.membership ?: return null
        if (m.isHome) return member.state
        return instances[m.structureId]?.state
    }

    /**
     * Forgets everything; on server stop. Tickets are not persisted, so they go with the server.
     */
    fun clear() {
        instances.clear()
        breaking.clear()
        unverified.clear()
    }

    /**
     * Call every server tick: verifies pending structures ([verifyPending]) and, every [TICKET_REFRESH] ticks,
     * refreshes home chunk tickets.
     */
    fun tick(gameTime: Long) {
        verifyPending()
        if (gameTime % TICKET_REFRESH == 0L) instances.values.forEach(::refreshTicket)
    }

    private fun refreshTicket(instance: MultiblockInstance) {
        if (!instance.shape.hasState) return
        val home = instance.homeChunk
        val needed = instance.loadedMembers().keys.any { offset ->
            val pos = instance.posFor(offset)
            ChunkCoord.of(pos) != home && tickets.isTicking(instance.level, pos)
        }
        if (needed) tickets.refresh(instance.level, home)
    }

    /**
     * Called once when a member attaches to a loaded level (placement or chunk load). A member that remembers a
     * structure re-registers into it (no re-validation, no new id) and is verified against the home member; otherwise
     * an auto-forming member tries to form a structure at every slot its roles could occupy.
     */
    fun onPartLoaded(level: ILevel, pos: BlockPos, member: IMultiblockMember) {
        val membership = member.membership
        if (membership != null) {
            val instance = instances.getOrPut(membership.structureId) {
                unverified += membership.structureId
                MultiblockInstance(membership.structureId, membership.shape, level, pos.subtract(membership.offset))
            }
            register(instance, membership.offset, member)
            verify(instance)
            return
        }
        if (!member.autoForm) return
        for (roleRef in member.candidateRoles) {
            val offsets = roleRef.shape.offsetsByRole[roleRef.role] ?: continue
            for (offset in offsets) {
                if (tryForm(level, roleRef.shape, pos.subtract(offset), autoOnly = true) != null) return
            }
        }
    }

    /**
     * The member's chunk unloaded; it is still part of its structure. Tells no sibling: the member's saved membership
     * remembers the structure for next time.
     */
    fun onPartUnloaded(member: IMultiblockMember) {
        val membership = member.membership ?: return
        val instance = instances[membership.structureId] ?: return
        if (instance.memberAt(membership.offset) !== member) return
        unregister(instance, membership.offset)
        if (instance.isEmpty()) forget(membership.structureId)
    }

    /**
     * The member at [pos] was broken, so its structure no longer exists. The shared state gets
     * [IMultiblockState.onBreak] (the home member is looked up through the level if it is not registered), then:
     * - [MultiblockBreakPolicy.DISSOLVE]: every other loaded member leaves.
     * - [MultiblockBreakPolicy.DESTROY_ALL]: every other slot is looked up through [level] (loading its chunk if
     *   needed), and each block there that still belongs to this structure leaves and is destroyed.
     */
    fun onPartRemoved(level: ILevel, pos: BlockPos, member: IMultiblockMember) {
        val membership = member.membership ?: return
        val id = membership.structureId
        // A sibling being destroyed below reports its own removal; the structure is already being torn down.
        if (!breaking.add(id)) return
        try {
            val instance = instances[id]
            val anchor = pos.subtract(membership.offset)
            val home = if (membership.isHome) member else instance?.home ?: memberIn(level, anchor, id, load = true)
            home?.state?.onBreak(level, anchor, pos)
            when (membership.shape.breakPolicy) {
                MultiblockBreakPolicy.DISSOLVE -> {
                    instance?.loadedMembers()?.filterKeys { it != membership.offset }?.values?.toList()?.forEach { it.leave() }
                    if (home !== member && home?.membership?.structureId == id) home.leave()
                }
                MultiblockBreakPolicy.DESTROY_ALL -> destroySiblings(level, anchor, membership)
            }
            forget(id)
        } finally {
            breaking.remove(id)
        }
    }

    /**
     * Forms [shape] at [anchor] from the members already there, whatever their [IMultiblockMember.autoForm]: every slot
     * must hold a loaded member with no structure that can fill that slot's role.
     *
     * @return The new structure, or null if any slot does not qualify (then no member joins).
     */
    fun form(level: ILevel, shape: MultiblockShape, anchor: BlockPos): MultiblockInstance? = tryForm(level, shape, anchor, autoOnly = false)

    /**
     * Ends [member]'s structure without breaking anything: no [IMultiblockState.onBreak], no destroyed blocks. Every
     * loaded member leaves (the home member is looked up through the level, loading its chunk, so its state is
     * discarded); unloaded members leave when they next load. For replacing a structure's blocks, e.g. with another
     * structure.
     */
    fun disband(level: ILevel, pos: BlockPos, member: IMultiblockMember) {
        val membership = member.membership ?: return
        val id = membership.structureId
        val members = LinkedHashSet<IMultiblockMember>()
        instances[id]?.loadedMembers()?.values?.let(members::addAll)
        members += member
        if (!membership.isHome) memberIn(level, pos.subtract(membership.offset), id, load = true)?.let { members += it }
        forget(id)
        members.filter { it.membership?.structureId == id }.forEach { it.leave() }
    }

    /**
     * Checks the structures whose members loaded before their home member could be checked. Run by [tick].
     */
    fun verifyPending() {
        if (unverified.isEmpty()) return
        unverified.toList().forEach { id -> instances[id]?.let(::verify) ?: unverified.remove(id) }
    }

    private fun verify(instance: MultiblockInstance) {
        if (instance.id !in unverified) return
        if (instance.home != null) {
            unverified -= instance.id
            return
        }
        // Never load the home chunk to check; stateful structures hold a ticket on it, so it loads soon.
        if (!instance.level.isLoaded(instance.anchorPos)) return
        val home = memberIn(instance.level, instance.anchorPos, instance.id, load = false)
        if (home != null) {
            unverified -= instance.id
            return
        }
        // The home member is gone or belongs elsewhere: the structure broke while these members were unloaded.
        val members = instance.loadedMembers().values.toList()
        forget(instance.id)
        members.forEach { it.leave() }
    }

    private fun register(instance: MultiblockInstance, offset: BlockPos, member: IMultiblockMember) {
        if (instance.memberAt(offset) === member) return
        if (instance.memberAt(offset) != null) unregister(instance, offset)
        instance.register(offset, member)
        if (offset == BlockPos.ZERO) unverified -= instance.id
        // Don't wait for the next refresh: the member may want the state right away.
        refreshTicket(instance)
    }

    private fun unregister(instance: MultiblockInstance, offset: BlockPos) = instance.unregister(offset)

    /**
     * Drops the structure from memory. Its home chunk ticket, if any, lapses on its own.
     */
    private fun forget(id: UUID) {
        instances.remove(id)
        unverified -= id
    }

    /**
     * The member at [pos] if it belongs to structure [id]. With [load], the lookup loads the chunk.
     */
    private fun memberIn(level: ILevel, pos: BlockPos, id: UUID, load: Boolean): IMultiblockMember? {
        if (!load && !level.isLoaded(pos)) return null
        val member = level.getIBlockEntity(pos)?.getModule(Modules.MULTIBLOCK_MEMBER, null) ?: return null
        return member.takeIf { it.membership?.structureId == id }
    }

    private fun destroySiblings(level: ILevel, anchor: BlockPos, membership: MultiblockMembership) {
        for (offset in membership.shape.slots.keys) {
            if (offset == membership.offset) continue
            val siblingPos = anchor.offset(offset)
            // Load the chunk if needed, or an unloaded sibling is orphaned. Another structure (or a lone part) may
            // have taken the slot since; leave it alone.
            val sibling = memberIn(level, siblingPos, membership.structureId, load = true) ?: continue
            sibling.leave()
            destroyBlock(level, siblingPos)
        }
    }

    private fun tryForm(level: ILevel, shape: MultiblockShape, anchor: BlockPos, autoOnly: Boolean): MultiblockInstance? {
        val found = LinkedHashMap<BlockPos, IMultiblockMember>()
        for ((offset, role) in shape.slots) {
            val at = anchor.offset(offset)
            if (!level.isLoaded(at)) return null
            val candidate = level.getIBlockEntity(at)?.getModule(Modules.MULTIBLOCK_MEMBER, null) ?: return null
            if (candidate.membership != null) return null
            if (autoOnly && !candidate.autoForm) return null
            if (MultiblockRoleRef(shape, role) !in candidate.candidateRoles) return null
            found[offset] = candidate
        }

        val id = UUID.randomUUID()
        val instance = MultiblockInstance(id, shape, level, anchor)
        instances[id] = instance
        found.forEach { (offset, member) ->
            member.join(MultiblockMembership(id, shape, offset))
            register(instance, offset, member)
        }
        return instance
    }

    companion object {
        /**
         * The server's manager. ItszuLib forwards server ticks (verification) and server stop (clear) to it.
         */
        @JvmField
        val SERVER = MultiblockManager()

        /**
         * Ticks a home chunk ticket lasts without a refresh.
         */
        const val TICKET_TIMEOUT = 60L

        /**
         * Ticks between ticket refreshes.
         */
        const val TICKET_REFRESH = 20L
    }
}

/**
 * Decides which faces of a multiblock part touch the rest of its structure. Shared by
 * [MultiblockSidedItemStorageConfiguration] and [MultiblockSidedFluidStorageConfiguration].
 */
class MultiblockFaces(
    private val level: () -> ILevel?,
    private val pos: () -> BlockPos,
    private val member: IMultiblockMember,
    private val front: () -> Direction,
) {
    fun internal(absolute: Direction): Boolean = level()?.let { isFacingSameStructure(it, pos(), absolute, member) } ?: false

    fun internalRelative(relative: Direction): Boolean = internal(DirectionUtil.getAbsoluteDirectionFromHorizontalRelative(relative, front()))

    companion object {
        /**
         * @return True if the block on [facing] of [pos] belongs to the same formed structure as [member].
         */
        @JvmStatic
        fun isFacingSameStructure(level: ILevel, pos: BlockPos, facing: Direction, member: IMultiblockMember): Boolean {
            val id = member.membership?.structureId ?: return false
            val neighbour = pos.relative(facing)
            // Never load a chunk to answer this: a block entity lookup in an unloaded chunk loads it synchronously.
            if (!level.isLoaded(neighbour)) return false
            val other = level.getIBlockEntity(neighbour)?.getModule(Modules.MULTIBLOCK_MEMBER, null) ?: return false
            return other.membership?.structureId == id
        }
    }
}

/**
 * Sided item configuration of a multiblock part: faces touching the rest of the same structure expose
 * [emptyStorage]'s storage (normally an empty one), do no automatic IO and cannot be reconfigured. Port of ItszuLib
 * 1.12.2's `MultiblockSidedItemStorageConfiguration`.
 *
 * Relative and absolute queries agree: an internal face reports [emptyStorage] and [EnumAutomaticIO.NONE] either way.
 */
open class MultiblockSidedItemStorageConfiguration(
    level: () -> ILevel?,
    pos: () -> BlockPos,
    member: IMultiblockMember,
    private val emptyStorage: String,
    defaults: (Direction) -> String,
    storages: Map<String, IItemStorage>,
    front: () -> Direction,
) : SidedItemStorageConfiguration(defaults, storages, front) {
    private val faces = MultiblockFaces(level, pos, member, front)

    override fun getStorageNameForAbsoluteFacing(direction: Direction): String =
        if (faces.internal(direction)) emptyStorage else super.getStorageNameForAbsoluteFacing(direction)

    override fun getStorageNameForRelativeFacing(direction: Direction): String =
        if (faces.internalRelative(direction)) emptyStorage else super.getStorageNameForRelativeFacing(direction)

    override fun getIOForAbsoluteFacing(direction: Direction): EnumAutomaticIO =
        if (faces.internal(direction)) EnumAutomaticIO.NONE else super.getIOForAbsoluteFacing(direction)

    override fun getIOForRelativeFacing(direction: Direction): EnumAutomaticIO =
        if (faces.internalRelative(direction)) EnumAutomaticIO.NONE else super.getIOForRelativeFacing(direction)

    override fun cycleRelativeFacingStorageForward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingStorageForward(direction)
    }

    override fun cycleRelativeFacingStorageBackward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingStorageBackward(direction)
    }

    override fun cycleRelativeFacingIOForward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingIOForward(direction)
    }

    override fun cycleRelativeFacingIOBackward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingIOBackward(direction)
    }
}

/**
 * Fluid counterpart of [MultiblockSidedItemStorageConfiguration].
 */
open class MultiblockSidedFluidStorageConfiguration(
    level: () -> ILevel?,
    pos: () -> BlockPos,
    member: IMultiblockMember,
    private val emptyStorage: String,
    defaults: (Direction) -> String,
    storages: Map<String, IFluidStorage>,
    front: () -> Direction,
) : SidedFluidStorageConfiguration(defaults, storages, front) {
    private val faces = MultiblockFaces(level, pos, member, front)

    override fun getStorageNameForAbsoluteFacing(direction: Direction): String =
        if (faces.internal(direction)) emptyStorage else super.getStorageNameForAbsoluteFacing(direction)

    override fun getStorageNameForRelativeFacing(direction: Direction): String =
        if (faces.internalRelative(direction)) emptyStorage else super.getStorageNameForRelativeFacing(direction)

    override fun getIOForAbsoluteFacing(direction: Direction): EnumAutomaticIO =
        if (faces.internal(direction)) EnumAutomaticIO.NONE else super.getIOForAbsoluteFacing(direction)

    override fun getIOForRelativeFacing(direction: Direction): EnumAutomaticIO =
        if (faces.internalRelative(direction)) EnumAutomaticIO.NONE else super.getIOForRelativeFacing(direction)

    override fun cycleRelativeFacingStorageForward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingStorageForward(direction)
    }

    override fun cycleRelativeFacingStorageBackward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingStorageBackward(direction)
    }

    override fun cycleRelativeFacingIOForward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingIOForward(direction)
    }

    override fun cycleRelativeFacingIOBackward(direction: Direction) {
        if (!faces.internalRelative(direction)) super.cycleRelativeFacingIOBackward(direction)
    }
}
