package com.itszuvalex.itszulib.core.frag

import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.utility.Loc4
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.core.IBlockEntityFragment
import com.itszuvalex.itszulib.core.INetworkNode
import com.itszuvalex.itszulib.core.TileNetwork
import com.itszuvalex.itszulib.util.FaceBitSet
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * Which faces of a block are connected to something, and which are blocked (a blocked face never connects). Saved and
 * synced to clients, e.g. for a conduit's model. Port of ItszuLib 1.12.2's `ModuleSidedConnectable` and
 * `ModuleSidedBlockableConnectable` (blocking is optional: never call [block] and nothing is blocked).
 *
 * The null face is never connected and always counts as blocked.
 */
open class FragConnectable @JvmOverloads constructor(private val name: String = NAME) : InternalBlockEntityFragment() {
    @JvmField
    val connections = FaceBitSet()

    @JvmField
    val blocked = FaceBitSet()

    fun isConnected(face: Direction?): Boolean = face != null && connections[face]

    fun isBlocked(face: Direction?): Boolean = face == null || blocked[face]

    /**
     * Sets [face]'s connected flag, saving and syncing if it changed. Ignores blocked faces when connecting.
     *
     * @return False if [face] is null, or blocked and [connected] is true.
     */
    open fun setConnected(face: Direction?, connected: Boolean): Boolean {
        if (face == null) return false
        if (connected && blocked[face]) return false
        if (connections[face] != connected) {
            connections[face] = connected
            markDirtyAndSync()
        }
        return true
    }

    /**
     * @return False if [face] is null or blocked.
     */
    open fun connect(face: Direction?): Boolean = setConnected(face, true)

    /**
     * @return False if [face] is null or blocked (a blocked face is already disconnected).
     */
    open fun disconnect(face: Direction?): Boolean = if (face == null || blocked[face]) false else setConnected(face, false)

    /**
     * Blocks [face] and disconnects it.
     *
     * @return Whether [face] was connected before; false if [face] is null.
     */
    open fun block(face: Direction?): Boolean {
        if (face == null) return false
        val was = connections[face]
        connections.clear(face)
        blocked.set(face)
        markDirtyAndSync()
        return was
    }

    /**
     * @return False only if [face] is null.
     */
    open fun unblock(face: Direction?): Boolean {
        if (face == null) return false
        if (blocked[face]) {
            blocked.clear(face)
            markDirtyAndSync()
        }
        return true
    }

    override fun name(): String = name

    override fun handlesScope(scope: NBTSerializationScope): Boolean = scope != NBTSerializationScope.ITEM

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) {
        output.putInt(CONNECTIONS_KEY, connections.bits)
        output.putInt(BLOCKED_KEY, blocked.bits)
    }

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) {
        connections.load(input.getIntOr(CONNECTIONS_KEY, 0))
        blocked.load(input.getIntOr(BLOCKED_KEY, 0))
    }

    companion object {
        const val NAME = "Connectable"
        const val CONNECTIONS_KEY = "Con"
        const val BLOCKED_KEY = "Block"
    }
}

/**
 * A network node whose connections are saved with the block, so the network can be rebuilt as chunks load. Port of
 * ItszuLib 1.12.2's `IPersistedConnectableNetworkNode`.
 */
interface IPersistedConnectableNetworkNode<C : IPersistedConnectableNetworkNode<C, N>, N : com.itszuvalex.itszulib.core.INetwork<C, N>> :
    INetworkNode<C, N> {
    /**
     * Connects to the node at [loc] (a neighbour), if both sides allow it.
     */
    fun addPersistedConnection(loc: Loc4)

    /**
     * Disconnects from the node at [loc], on both sides.
     */
    fun removePersistedConnection(loc: Loc4)
}

/**
 * A wire: a block that joins its neighbouring wires of the same kind into a [TileNetwork], remembering which faces are
 * connected ([FragConnectable]) and exposing itself as the network node module [module]. Port of ItszuLib 1.12.2's
 * `ModuleNetworkedWire`. Server side only; on the client it just carries the synced connection flags.
 *
 * Lifecycle: [onLoad] creates or joins a network and reconnects faces whose neighbours are loaded;
 * [onNeighborChanged] connects to new neighbours and drops removed ones; [onRemove] (block broken) disconnects the
 * neighbours; [invalidateFrags] (chunk unload or removal) leaves the network. Never loads a chunk.
 *
 * Differences from 1.12.2:
 * - Disconnecting clears the flag on both wires. 1.12.2 cleared only its own, so the neighbour reconnected on the next
 *   load.
 * - Faces are reconciled with the neighbours on load as well as on placement, so a neighbour removed while this chunk
 *   was unloaded no longer leaves a stale connected flag.
 * - Neighbours are only looked up in loaded chunks (1.12.2 force-loaded them when checking a face).
 *
 * @param C The concrete wire class (the node type).
 * @param N Its network type.
 * @param networkFactory Creates an empty network for a wire that has none.
 */
abstract class FragNetworkedWire<C : FragNetworkedWire<C, N>, N : TileNetwork<C, N>>(
    private val networkFactory: () -> N,
    name: String = NAME,
) : FragConnectable(name), IBlockEntityFragment<C>, IPersistedConnectableNetworkNode<C, N> {
    private var currentNetwork: N? = null
    private var level: ILevel? = null

    @Suppress("UNCHECKED_CAST")
    private fun self(): C = this as C

    /**
     * @return Whether this wire should connect to [other], the wire on its [face]. Both wires are asked.
     */
    protected open fun shouldConnect(face: Direction, other: C): Boolean = true

    /**
     * @return The wire node [be] exposes on [side] (its face towards this wire), or null.
     */
    protected open fun nodeOf(be: IBlockEntity, side: Direction): C? = be.getModule(module(), side)

    override fun faceToModuleMapper(be: IBlockEntity): (Direction?) -> C? = { self() }

    private fun pos(): BlockPos = host?.blockEntity()?.getBlockPos() ?: BlockPos.ZERO

    private fun faceTowards(loc: Loc4): Direction? =
        if (loc.dimensionId != getLoc().dimensionId) null else Direction.entries.firstOrNull { pos().relative(it) == loc.pos }

    private fun neighbour(face: Direction): C? {
        val level = level ?: return null
        val at = pos().relative(face)
        if (!level.isLoaded(at)) return null
        val node = level.getIBlockEntity(at)?.let { nodeOf(it, face.opposite) } ?: return null
        // A neighbour placed this tick has not had onLoad yet; it is in the same level.
        if (node.level == null) node.level = level
        return node
    }

    private fun wants(face: Direction, other: C): Boolean =
        !isBlocked(face) && !other.isBlocked(face.opposite) && shouldConnect(face, other) && other.shouldConnect(face.opposite, self())

    private fun linked(other: C): Boolean = currentNetwork?.getConnections(getLoc())?.any { it == other.getLoc() } == true

    /**
     * Makes [face]'s connection match its neighbour: connects to a willing wire, disconnects otherwise. Leaves faces
     * whose neighbour is in an unloaded chunk alone.
     */
    fun checkFace(face: Direction) {
        val level = level ?: return
        if (level.isClientSide() || !level.isLoaded(pos().relative(face))) return
        val other = neighbour(face)
        if (other != null && wants(face, other)) {
            if (!isConnected(face) || !other.isConnected(face.opposite) || !linked(other)) link(face, other)
        } else if (isConnected(face)) {
            unlink(face, other)
        }
    }

    /**
     * [checkFace] for every face.
     */
    fun refreshConnections() = Direction.entries.forEach(::checkFace)

    private fun link(face: Direction, other: C) {
        setConnected(face, true)
        other.setConnected(face.opposite, true)
        val network = currentNetwork ?: other.getNetwork() ?: newNetwork()
        network.addConnectionNodes(self(), other)
    }

    private fun unlink(face: Direction, other: C?) {
        setConnected(face, false)
        if (other == null) return
        other.setConnected(face.opposite, false)
        val network = currentNetwork
        if (network != null && network === other.getNetwork()) network.removeConnectionNodes(self(), other)
    }

    private fun newNetwork(): N = networkFactory().also { it.register() }

    private fun ensureNetwork() {
        if (currentNetwork != null) return
        val network = newNetwork()
        network.addNode(self())
    }

    override fun onLoad(level: ILevel, pos: BlockPos) {
        if (level.isClientSide()) return
        this.level = level
        ensureNetwork()
        refreshConnections()
    }

    override fun onNeighborChanged(level: ILevel, pos: BlockPos) {
        if (level.isClientSide() || currentNetwork == null) return
        this.level = level
        refreshConnections()
    }

    override fun onRemove(level: ILevel, pos: BlockPos, blockStatePrev: BlockState) {
        if (level.isClientSide()) return
        this.level = level
        for (face in connections.faces()) neighbour(face)?.setConnected(face.opposite, false)
        leaveNetwork()
    }

    override fun invalidateFrags() = leaveNetwork()

    private fun leaveNetwork() {
        currentNetwork?.removeNode(self())
        currentNetwork = null
    }

    override fun block(face: Direction?): Boolean {
        val other = face?.let(::neighbour)
        val was = super.block(face)
        if (face != null && other != null) unlink(face, other)
        return was
    }

    override fun unblock(face: Direction?): Boolean {
        val ok = super.unblock(face)
        if (face != null && currentNetwork != null) checkFace(face)
        return ok
    }

    override fun addPersistedConnection(loc: Loc4) {
        faceTowards(loc)?.let(::checkFace)
    }

    override fun removePersistedConnection(loc: Loc4) {
        val face = faceTowards(loc) ?: return
        if (isConnected(face)) unlink(face, neighbour(face))
    }

    override fun setNetwork(network: N) {
        currentNetwork = network
    }

    override fun getNetwork(): N? = currentNetwork

    override fun getLoc(): Loc4 = Loc4.of(level ?: error("Wire at ${pos()} is not loaded"), pos())

    override fun refresh() {}

    override fun canConnect(loc: Loc4): Boolean = faceTowards(loc)?.let { !isBlocked(it) } ?: false

    override fun canAdd(network: N): Boolean = true

    override fun onAdded(network: N) {}

    override fun onRemoved(network: N) {
        if (currentNetwork === network) currentNetwork = null
    }

    override fun onConnect(loc: Loc4) {}

    override fun onDisconnect(loc: Loc4) {}

    companion object {
        const val NAME = "NetworkedWire"
    }
}
