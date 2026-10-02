package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.storage.PowerBattery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private fun battery(max: Double, stored: Double) = PowerBattery(max).also { it.setStorage(stored) }

private fun res(b: PowerBattery, rate: Double = 1000.0) = DistributableBattery(b) { rate }

class DistributionAlgorithmTests {
    @Test
    fun Distribute_ProducerToConsumer_RespectsTransferRates() {
        val producer = battery(1000.0, 500.0)
        val consumer = battery(1000.0, 0.0)
        val result = DistributionAlgorithm(listOf(res(producer, 50.0)), listOf(), listOf(res(consumer, 30.0))).distribute()
        assertEquals(30.0, consumer.storage())
        assertEquals(470.0, producer.storage())
        assertEquals(30.0, result.toConsumers)
        assertEquals(30.0, result.fromProducers)
    }

    @Test
    fun Distribute_HigherPriority_GoesFirstWithinItsRole() {
        // Without priorities the producer with less room (the fuller one, a) would give first.
        val a = battery(1000.0, 900.0)
        val b = battery(1000.0, 100.0)
        val preferred = object : Distributable by res(b) {
            override val priority: Double get() = 1.0
        }
        val low = battery(1000.0, 0.0)
        val high = battery(1000.0, 500.0)
        val urgent = object : Distributable by res(high) {
            override val priority: Double get() = 1.0
        }
        val result = DistributionAlgorithm(listOf(res(a, 60.0), preferred), listOf(), listOf(res(low), urgent)).distribute()
        assertEquals(160.0, result.toConsumers)
        assertEquals(0.0, b.storage(), "the preferred producer gave everything")
        assertEquals(840.0, a.storage())
        assertEquals(660.0, high.storage(), "the urgent consumer filled first")
        assertEquals(0.0, low.storage())
    }

    @Test
    fun Distribute_Surplus_GoesToStorage() {
        val producer = battery(1000.0, 500.0)
        val storage = battery(1000.0, 0.0)
        val consumer = battery(1000.0, 990.0)
        DistributionAlgorithm(listOf(res(producer, 100.0)), listOf(res(storage)), listOf(res(consumer))).distribute()
        assertEquals(1000.0, consumer.storage())
        assertEquals(90.0, storage.storage())
        assertEquals(400.0, producer.storage())
    }

    @Test
    fun Distribute_Shortfall_StorageTopsUpConsumers() {
        val producer = battery(1000.0, 10.0)
        val storage = battery(1000.0, 500.0)
        val consumer = battery(1000.0, 0.0)
        DistributionAlgorithm(listOf(res(producer)), listOf(res(storage)), listOf(res(consumer, 100.0))).distribute()
        assertEquals(100.0, consumer.storage())
        assertEquals(0.0, producer.storage())
        assertEquals(410.0, storage.storage())
    }

    @Test
    fun Distribute_StorageOnly_DoesNotShuffleBetweenStorages() {
        val a = battery(1000.0, 500.0)
        val b = battery(1000.0, 0.0)
        val result = DistributionAlgorithm(listOf(), listOf(res(a), res(b)), listOf()).distribute()
        assertEquals(0.0, result.total)
        assertEquals(500.0, a.storage())
    }

    /**
     * Removing only what the sink accepted: nothing is destroyed.
     */
    @Test
    fun Distribute_SinkAcceptsLess_SourceKeepsTheRest() {
        val producer = battery(1000.0, 500.0)
        val stingy = object : Distributable {
            var got = 0.0
            override val max = 100.0
            override val amount get() = 0.0
            override val transferMax = 100.0
            override fun add(amount: Double): Double = minOf(amount, 10.0).also { got += it }
            override fun remove(amount: Double): Double = 0.0
        }
        DistributionAlgorithm(listOf(res(producer)), listOf(), listOf(stingy)).distribute()
        assertEquals(10.0, stingy.got)
        assertEquals(490.0, producer.storage())
    }

    @Test
    fun Distribute_NothingToGive_ReturnsNone() {
        val result = DistributionAlgorithm(listOf(res(battery(100.0, 0.0))), listOf(), listOf(res(battery(100.0, 0.0)))).distribute()
        assertEquals(DistributionAlgorithm.Result.NONE, result)
    }

    @Test
    fun DistributeParticipants_SplitsByRole() {
        val producer = battery(1000.0, 500.0)
        val consumer = battery(1000.0, 0.0)
        val result = DistributionAlgorithm.distribute(listOf(
            DistributionParticipant("p", DistributionRole.PRODUCER, res(producer, 40.0)),
            DistributionParticipant("c", DistributionRole.CONSUMER, res(consumer)),
        ))
        assertEquals(40.0, consumer.storage())
        assertEquals(40.0, result.fromProducers)
    }
}
