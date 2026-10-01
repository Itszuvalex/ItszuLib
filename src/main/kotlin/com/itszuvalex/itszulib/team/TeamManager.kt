package com.itszuvalex.itszulib.team

import com.mojang.logging.LogUtils
import com.mojang.serialization.DynamicOps
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag

/**
 * The server's teams: the one place team state changes. Everything else reads [state] (an immutable snapshot) or asks
 * for a change through [change].
 *
 * - [change] runs one operation against the current state. The operation either returns a new valid state, which
 *   replaces the old one and marks the data dirty, or throws ([TeamException] for a refused request), leaving the
 *   state exactly as it was. There is no half-applied change.
 * - Changes must happen on the thread that [load]ed the data (the server thread).
 * - [save] writes the current snapshot through the [TeamStore], only when something changed.
 */
class TeamManager {
    @Volatile
    var state: TeamState = TeamState.EMPTY
        private set

    private var store: TeamStore? = null
    private var ops: DynamicOps<Tag> = NbtOps.INSTANCE
    private var owner: Thread? = null
    private var dirty = false
    private val listeners = ArrayList<(old: TeamState, new: TeamState) -> Unit>()

    val isLoaded: Boolean get() = store != null

    /**
     * Whether changes will reach disk; false after a load that could not read the saved data (see [TeamStore]).
     */
    val isPersistent: Boolean get() = store?.writable == true

    /**
     * @param ops Ops for data type codecs, usually the server's registry-aware NBT ops.
     */
    fun load(store: TeamStore, ops: DynamicOps<Tag>) {
        check(this.store == null) { "Teams are already loaded from ${this.store?.file}" }
        this.store = store
        this.ops = ops
        state = store.load(ops)
        owner = Thread.currentThread()
        dirty = false
    }

    /**
     * Applies [operation] to the current state. On success the new state replaces it and [onChange] listeners run.
     *
     * @throws TeamException if a rule refused the operation; nothing changed.
     */
    fun change(operation: (TeamState) -> TeamState): TeamState {
        check(store != null) { "Teams are not loaded" }
        check(Thread.currentThread() === owner) { "Teams can only change on the server thread" }
        val old = state
        val new = operation(old)
        if (new === old) return old
        state = new
        dirty = true
        // The change has happened; a failing listener (e.g. a sync) is logged, not reported as a failed change.
        for (listener in listeners) {
            try {
                listener(old, new)
            } catch (e: Exception) {
                LOGGER.error("Team change listener failed", e)
            }
        }
        return new
    }

    /**
     * Runs after every successful [change], with the states before and after.
     */
    fun onChange(listener: (old: TeamState, new: TeamState) -> Unit) {
        listeners += listener
    }

    /**
     * Writes the current state if it changed since the last save. A failed write is logged and retried at the next
     * save; the data stays dirty.
     */
    fun save() {
        val store = store ?: return
        if (!dirty) return
        val snapshot = state
        try {
            if (store.save(snapshot, ops) && state === snapshot) dirty = false
        } catch (e: Exception) {
            LOGGER.error("Could not save team data to {}", store.file, e)
        }
    }

    /**
     * Forgets the loaded data, e.g. when the server stops. Call [save] first.
     */
    fun unload() {
        store = null
        owner = null
        state = TeamState.EMPTY
        dirty = false
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
    }
}
