package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.adapters.IBattery
import net.neoforged.fml.LogicalSide
import kotlin.math.min

/**
 * An amount of something that a [DistributionAlgorithm] can move into or out of, with a per-tick limit: a battery,
 * a tank, a buffer of computation. Amounts are doubles; what they measure is up to the user.
 */
interface Distributable {
    val max: Double
    val amount: Double
    val room: Double get() = max - amount

    /** At most this much moves in or out per distribution. */
    val transferMax: Double

    /**
     * Higher goes first among participants of the same role, before the usual order (for example efficient computers
     * before wasteful ones, or renewable generators before fuel burners). 0 by default.
     */
    val priority: Double get() = 0.0

    /**
     * @return Amount actually added.
     */
    fun add(amount: Double): Double

    /**
     * @return Amount actually removed.
     */
    fun remove(amount: Double): Double
}

/**
 * An ItszuLib battery as a [Distributable], moving at most [transfer] per distribution.
 */
class DistributableBattery(private val battery: IBattery, private val transfer: () -> Double) : Distributable {
    override val max: Double get() = battery.maxStorage()
    override val amount: Double get() = battery.storage()
    override val transferMax: Double get() = transfer()
    override fun add(amount: Double): Double = battery.fill(amount)
    override fun remove(amount: Double): Double = battery.drain(amount)
}

/**
 * What a participant does in a distribution: producers only give, consumers only take, storage does both (taking
 * surplus, giving when producers fall short).
 */
enum class DistributionRole { PRODUCER, STORAGE, CONSUMER }

/**
 * One participant of a [DistributingTileNetwork]'s distribution. [key] identifies what it belongs to, so a block (or
 * multiblock) reached through several nodes takes part once.
 */
data class DistributionParticipant(val key: Any, val role: DistributionRole, val resource: Distributable)

/**
 * Producer/storage/consumer distribution in one pass: moves from producers (then storage) to consumers (then
 * storage), each within its transfer limit.
 *
 * Priorities: a higher [Distributable.priority] goes first within its role; among equal priorities, producers with
 * the least room give first (so generators do not fill up and stall), storage with the least room gives first, storage
 * with the least in it fills first, consumers with the least in them fill first. If
 * producers make at least what consumers can take, the surplus goes to storage; otherwise storage tops consumers up.
 * Each step removes from the source only what the sink actually accepted, so nothing is created or destroyed.
 */
class DistributionAlgorithm(
    private val producers: Collection<Distributable>,
    private val storage: Collection<Distributable>,
    private val consumers: Collection<Distributable>,
) {
    /**
     * Totals moved by one [distribute] call.
     */
    data class Result(val fromProducers: Double, val fromStorage: Double, val toConsumers: Double, val toStorage: Double) {
        val total: Double get() = toConsumers + toStorage

        companion object {
            @JvmField
            val NONE = Result(0.0, 0.0, 0.0, 0.0)
        }
    }

    private class Entry(val node: Distributable, var remaining: Double, val primary: Boolean)

    fun distribute(): Result {
        val byRoom = compareByDescending<Entry> { it.node.priority }.thenBy { it.node.room }
        val byAmount = compareByDescending<Entry> { it.node.priority }.thenBy { it.node.amount }
        val producerEntries = producers.map { Entry(it, min(it.amount, it.transferMax).coerceAtLeast(0.0), true) }.sortedWith(byRoom)
        val storageTake = storage.map { Entry(it, min(it.amount, it.transferMax).coerceAtLeast(0.0), false) }.sortedWith(byRoom)
        val storageGive = storage.map { Entry(it, min(it.room, it.transferMax).coerceAtLeast(0.0), false) }.sortedWith(byAmount)
        val consumerEntries = consumers.map { Entry(it, min(it.room, it.transferMax).coerceAtLeast(0.0), true) }.sortedWith(byAmount)

        val produced = producerEntries.sumOf { it.remaining }
        val stored = storageTake.sumOf { it.remaining }
        val storageRoom = storageGive.sumOf { it.remaining }
        val consumerRoom = consumerEntries.sumOf { it.remaining }

        if (produced <= 0 && stored <= 0) return Result.NONE
        if (consumerRoom <= 0 && storageRoom <= 0) return Result.NONE

        val toDistribute = if (produced >= consumerRoom) min(produced, storageRoom + consumerRoom) else min(consumerRoom, produced + stored)

        val sources = (producerEntries + storageTake).iterator()
        val sinks = (consumerEntries + storageGive).iterator()
        var source = sources.nextOrNull()
        var sink = sinks.nextOrNull()
        var distributed = 0.0
        var fromProducers = 0.0
        var fromStorage = 0.0
        var toConsumers = 0.0
        var toStorage = 0.0
        while (distributed < toDistribute && source != null && sink != null) {
            // A storage participant is never both the source and the sink of one step.
            if (source.node === sink.node) {
                sink = sinks.nextOrNull()
                continue
            }
            val shift = minOf(source.remaining, sink.remaining, toDistribute - distributed)
            val moved = if (shift > 0) source.node.remove(sink.node.add(shift)) else 0.0
            source.remaining -= shift
            sink.remaining -= shift
            distributed += moved
            if (source.primary) fromProducers += moved else fromStorage += moved
            if (sink.primary) toConsumers += moved else toStorage += moved
            if (moved < shift) {
                // The sink took less than offered: it is full.
                sink.remaining = 0.0
            }
            if (source.remaining <= 0) source = sources.nextOrNull()
            if (sink.remaining <= 0) sink = sinks.nextOrNull()
        }
        return Result(fromProducers, fromStorage, toConsumers, toStorage)
    }

    private fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null

    companion object {
        /**
         * Distributes among [participants] by role.
         */
        @JvmStatic
        fun distribute(participants: Collection<DistributionParticipant>): Result = DistributionAlgorithm(
            participants.filter { it.role == DistributionRole.PRODUCER }.map { it.resource },
            participants.filter { it.role == DistributionRole.STORAGE }.map { it.resource },
            participants.filter { it.role == DistributionRole.CONSUMER }.map { it.resource },
        ).distribute()
    }
}

/**
 * A network node that brings distribution participants (for example the machines attached to a conduit).
 */
interface IDistributionNode {
    fun distributionParticipants(): Sequence<DistributionParticipant>
}

/**
 * A [TileNetwork] that runs a [DistributionAlgorithm] over its participants at the end of every tick: power over
 * conduits, computation over cables, or anything else with producers, storage and consumers. By default the
 * participants are those of its [IDistributionNode] nodes, each [DistributionParticipant.key] once; override
 * [participants] to gather them differently.
 */
abstract class DistributingTileNetwork<C : INetworkNode<C, N>, N : DistributingTileNetwork<C, N>>(id: Int, side: LogicalSide) :
    TileNetwork<C, N>(id, side) {
    /** What the last distribution moved. */
    var lastResult: DistributionAlgorithm.Result = DistributionAlgorithm.Result.NONE
        private set

    open fun participants(): Collection<DistributionParticipant> {
        val seen = LinkedHashMap<Any, DistributionParticipant>()
        for (node in getNodes()) {
            if (node !is IDistributionNode) continue
            node.distributionParticipants().forEach { seen.putIfAbsent(it.key, it) }
        }
        return seen.values
    }

    override fun onTickEnd() {
        lastResult = DistributionAlgorithm.distribute(participants())
    }
}
