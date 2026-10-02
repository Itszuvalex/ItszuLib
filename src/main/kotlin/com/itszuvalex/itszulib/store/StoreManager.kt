package com.itszuvalex.itszulib.store

import com.mojang.logging.LogUtils
import com.mojang.serialization.DynamicOps
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.LevelResource
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent

/**
 * The one place a piece of server data changes. Everything else reads [state] (an immutable value) or asks for a
 * change through [change].
 *
 * - [change] runs one operation against the current state. The operation either returns a new state, which replaces
 *   the old one and marks the data dirty, or throws, leaving the state exactly as it was. There is no half-applied
 *   change, so a value that checks its own invariants on construction is always valid.
 * - Changes must happen on the thread that [load]ed the data (the server thread).
 * - [save] writes the current state through the [SafeStore], only when something changed.
 *
 * Usually registered with [ServerStores], which loads and saves it with the server.
 */
open class StoreManager<T>(private val empty: T) {
    @Volatile
    var state: T = empty
        private set

    private var store: SafeStore<T>? = null
    private var ops: DynamicOps<Tag> = NbtOps.INSTANCE
    private var owner: Thread? = null
    private var dirty = false
    private val listeners = ArrayList<(old: T, new: T) -> Unit>()

    val isLoaded: Boolean get() = store != null

    /**
     * Whether changes will reach disk; false after a load that could not read the saved data (see [SafeStore]).
     */
    val isPersistent: Boolean get() = store?.writable == true

    /**
     * @param ops Ops for the format's codecs, usually the server's registry-aware NBT ops.
     */
    fun load(store: SafeStore<T>, ops: DynamicOps<Tag>) {
        check(this.store == null) { "Already loaded from ${this.store?.file}" }
        this.store = store
        this.ops = ops
        state = store.load(ops)
        owner = Thread.currentThread()
        dirty = false
    }

    /**
     * Applies [operation] to the current state. On success the new state replaces it and [onChange] listeners run.
     * An exception from [operation] propagates and changes nothing.
     */
    fun change(operation: (T) -> T): T {
        check(store != null) { "Not loaded" }
        check(Thread.currentThread() === owner) { "Server data can only change on the server thread" }
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
                LOGGER.error("Change listener failed", e)
            }
        }
        return new
    }

    /**
     * Runs after every successful [change], with the states before and after.
     */
    fun onChange(listener: (old: T, new: T) -> Unit) {
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
            LOGGER.error("Could not save {} to {}", store.name, store.file, e)
        }
    }

    /**
     * Forgets the loaded data, e.g. when the server stops. Call [save] first.
     */
    fun unload() {
        store = null
        owner = null
        state = empty
        dirty = false
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
    }
}

/**
 * Ties [StoreManager]s to the server's lifecycle: each registered store loads when the server starts (from
 * `<world>/data/<namespace>/<path>`), saves with the overworld (autosave and shutdown), and is saved and unloaded when
 * the server stops. Register during mod construction.
 */
object ServerStores {
    private class Entry<T>(val id: Identifier, val manager: StoreManager<T>, val store: (java.nio.file.Path) -> SafeStore<T>)

    private val entries = ArrayList<Entry<*>>()
    private var listening = false

    /**
     * @param id Where the data lives: `<world>/data/<namespace>/<path>`, e.g. `examplemod:waypoints.dat`.
     */
    @JvmStatic
    fun <T> register(id: Identifier, manager: StoreManager<T>, format: StoreFormat<T>, name: String = id.toString()) =
        register(id, manager) { SafeStore(it, format, name) }

    /**
     * As [register], with a store of your own (e.g. a [SafeStore] subclass).
     */
    @JvmStatic
    fun <T> register(id: Identifier, manager: StoreManager<T>, store: (java.nio.file.Path) -> SafeStore<T>) {
        synchronized(entries) {
            require(entries.none { it.id == id }) { "Server store $id is already registered" }
            entries += Entry(id, manager, store)
            if (!listening) {
                listening = true
                NeoForge.EVENT_BUS.addListener(::onServerStarting)
                NeoForge.EVENT_BUS.addListener(::onLevelSave)
                NeoForge.EVENT_BUS.addListener(::onServerStopped)
            }
        }
    }

    private fun onServerStarting(event: ServerStartingEvent) {
        val server = event.server
        val ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE)
        for (entry in entries) load(entry, server.getWorldPath(LevelResource.DATA), ops)
    }

    private fun <T> load(entry: Entry<T>, data: java.nio.file.Path, ops: DynamicOps<Tag>) {
        val file = data.resolve(entry.id.namespace).resolve(entry.id.path)
        entry.manager.load(entry.store(file), ops)
    }

    private fun onLevelSave(event: LevelEvent.Save) {
        val level = event.level as? ServerLevel ?: return
        if (level.dimension() == Level.OVERWORLD) entries.forEach { it.manager.save() }
    }

    private fun onServerStopped(event: ServerStoppedEvent) {
        for (entry in entries) {
            entry.manager.save()
            entry.manager.unload()
        }
    }
}
