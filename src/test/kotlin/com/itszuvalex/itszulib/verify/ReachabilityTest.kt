package com.itszuvalex.itszulib.verify

import com.itszuvalex.itszulib.verify.Reachability.Producer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReachabilityTest {
    private fun p(source: String, makes: String, vararg needs: Set<String>) = Producer(source, needs.toList(), setOf(makes))

    @Test
    fun Reachable_Chain_FollowsEveryStep() {
        val reached = Reachability.reachable(setOf("ore"), listOf(p("smelt", "ingot", setOf("ore")), p("craft", "gear", setOf("ingot"))))
        assertEquals(setOf("ore", "ingot", "gear"), reached)
    }

    @Test
    fun Reachable_AnyOneOfTheAlternativesIsEnough() {
        val reached = Reachability.reachable(setOf("b"), listOf(p("r", "x", setOf("a", "b"))))
        assertTrue("x" in reached)
    }

    @Test
    fun Reachable_EveryNeedMustBeMet() {
        val reached = Reachability.reachable(setOf("a"), listOf(p("r", "x", setOf("a"), setOf("b"))))
        assertEquals(setOf("a"), reached)
    }

    @Test
    fun Reachable_ProducerWithoutNeeds_AlwaysRuns() {
        assertEquals(setOf("x"), Reachability.reachable(emptySet(), listOf(p("free", "x"))))
    }

    @Test
    fun Reachable_ItemsOnlyMadeFromEachOther_StayUnreachable() {
        val reached = Reachability.reachable(
            setOf("stone"),
            listOf(p("a", "x", setOf("y")), p("b", "y", setOf("x"))),
        )
        assertEquals(setOf("stone"), reached)
    }

    @Test
    fun Reachable_OrderOfProducersDoesNotMatter() {
        val producers = listOf(p("c", "gear", setOf("ingot")), p("s", "ingot", setOf("ore")))
        assertEquals(Reachability.reachable(setOf("ore"), producers), Reachability.reachable(setOf("ore"), producers.reversed()))
    }

    @Test
    fun BlockedBy_NoProducer_SaysNothingMakesIt() {
        assertEquals(listOf("nothing makes it"), Reachability.blockedBy("x", setOf("a"), emptyList<Producer<String>>()))
    }

    @Test
    fun BlockedBy_NamesTheUnmetNeeds() {
        val producers = listOf(p("recipe 1", "x", setOf("a"), setOf("b", "c")))
        assertEquals(listOf("recipe 1 needs b or c"), Reachability.blockedBy("x", setOf("a"), producers))
    }
}
