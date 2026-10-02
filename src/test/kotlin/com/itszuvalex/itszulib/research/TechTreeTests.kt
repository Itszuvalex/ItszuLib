package com.itszuvalex.itszulib.research

import com.itszuvalex.itszulib.client.screen.TechTreeGeometry
import com.itszuvalex.itszulib.team.Research
import net.minecraft.SharedConstants
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Optional

private val TREE = Identifier.fromNamespaceAndPath("itszulib_test", "tree")

private fun id(path: String) = Identifier.fromNamespaceAndPath("itszulib_test", path)

private fun tech(vararg prerequisites: String, cost: Long = 10L, hidden: Boolean = false, default: Boolean = false, position: TechTreeLayout.Point? = null) =
    Technology(TREE, prerequisites.map(::id), cost, Items.BOOK, Optional.empty(), Optional.empty(), Optional.ofNullable(position), hidden, default)

private fun techs(vararg entries: Pair<String, Technology>) = Technologies(entries.associate { id(it.first) to it.second })

class TechTreeTests {
    companion object {
        @BeforeAll
        @JvmStatic
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    @Test
    fun State_FollowsPrerequisites() {
        val t = techs("root" to tech(), "mid" to tech("root"), "leaf" to tech("mid"), "secret" to tech("mid", hidden = true))
        val none = Research.EMPTY
        Assertions.assertEquals(TechnologyState.AVAILABLE, t.state(id("root"), none))
        Assertions.assertEquals(TechnologyState.LOCKED, t.state(id("mid"), none))
        Assertions.assertEquals(TechnologyState.HIDDEN, t.state(id("secret"), none))
        val some = Research(setOf(id("root"), id("mid")))
        Assertions.assertEquals(TechnologyState.RESEARCHED, t.state(id("mid"), some))
        Assertions.assertEquals(TechnologyState.AVAILABLE, t.state(id("leaf"), some))
        Assertions.assertEquals(TechnologyState.AVAILABLE, t.state(id("secret"), some))
        Assertions.assertNull(t.state(id("nope"), some))
    }

    @Test
    fun State_UnlockedByDefault_CountsAsResearched() {
        val t = techs("free" to tech(default = true), "next" to tech("free"))
        Assertions.assertEquals(TechnologyState.RESEARCHED, t.state(id("free"), Research.EMPTY))
        Assertions.assertEquals(TechnologyState.AVAILABLE, t.state(id("next"), Research.EMPTY))
    }

    @Test
    fun State_UnknownPrerequisite_NeverAvailable() {
        val t = techs("orphan" to tech("missing"))
        Assertions.assertEquals(TechnologyState.LOCKED, t.state(id("orphan"), Research(setOf(id("missing")))))
        Assertions.assertEquals(listOf(id("missing")), t.missingPrerequisites(id("orphan"), Research.EMPTY))
    }

    @Test
    fun Problems_ReportsMissingPrerequisitesAndCycles() {
        val t = techs("a" to tech("c"), "b" to tech("a"), "c" to tech("b"), "d" to tech("ghost"), "ok" to tech())
        val problems = t.problems()
        Assertions.assertTrue(problems.any { "ghost" in it }, problems.toString())
        Assertions.assertEquals(1, problems.count { "cycle" in it }, problems.toString())
        Assertions.assertTrue(techs("ok" to tech(), "next" to tech("ok")).problems().isEmpty())
    }

    @Test
    fun Layout_ColumnsAreLongestPathFromARoot() {
        val t = techs("a" to tech(), "b" to tech("a"), "c" to tech("b"), "d" to tech("a", "c"))
        val layout = TechTreeLayout.layout(t.all)
        Assertions.assertEquals(listOf(0f, 1f, 2f, 3f), listOf("a", "b", "c", "d").map { layout.positions.getValue(id(it)).x })
    }

    @Test
    fun Layout_LinkSkippingColumns_GetsAWaypointPerColumn() {
        val t = techs("a" to tech(), "b" to tech("a"), "c" to tech("b"), "d" to tech("a", "c"))
        val edge = TechTreeLayout.layout(t.all).edges.single { it.from == id("a") && it.to == id("d") }
        Assertions.assertEquals(listOf(1f, 2f), edge.waypoints.map { it.x })
    }

    @Test
    fun Layout_NodesInAColumnAreAtLeastOneRowApart() {
        val entries = (1..6).map { "root$it" to tech() } + (1..6).map { "child$it" to tech("root${(it % 3) + 1}", "root${7 - it}") }
        val layout = TechTreeLayout.layout(techs(*entries.toTypedArray()).all)
        for ((_, column) in layout.positions.values.groupBy { it.x }) {
            val rows = column.map { it.y }.sorted()
            for (i in 1 until rows.size) Assertions.assertTrue(rows[i] - rows[i - 1] >= 0.999f, "rows $rows")
        }
        Assertions.assertEquals(0f, layout.positions.values.minOf { it.y })
    }

    @Test
    fun Layout_OrderingRemovesAvoidableCrossings() {
        // a1 -> b2 and a2 -> b1 cross unless the second column is reordered.
        val t = techs("a1" to tech(), "a2" to tech(), "b1" to tech("a2"), "b2" to tech("a1"))
        val p = TechTreeLayout.layout(t.all).positions
        val aAbove = p.getValue(id("a1")).y < p.getValue(id("a2")).y
        val bAbove = p.getValue(id("b2")).y < p.getValue(id("b1")).y
        Assertions.assertEquals(aAbove, bAbove)
    }

    @Test
    fun Layout_IsDeterministicAndHonoursFixedPositions() {
        val fixed = TechTreeLayout.Point(5f, 7f)
        val t = techs("a" to tech(), "b" to tech("a"), "c" to tech("a", position = fixed))
        Assertions.assertEquals(TechTreeLayout.layout(t.all), TechTreeLayout.layout(t.all))
        Assertions.assertEquals(fixed, TechTreeLayout.layout(t.all).positions.getValue(id("c")))
    }

    @Test
    fun Layout_Cycle_StillPlacesEveryNode() {
        val t = techs("a" to tech("b"), "b" to tech("a"), "c" to tech("a"))
        Assertions.assertEquals(3, TechTreeLayout.layout(t.all).positions.size)
    }

    @Test
    fun Geometry_NodeAt_FindsOnlyVisibleNodesUnderThePoint() {
        val t = techs("a" to tech(), "b" to tech("a"))
        val layout = TechTreeLayout.layout(t.all)
        val b = layout.positions.getValue(id("b"))
        val bx = TechTreeGeometry.nodeX(b, 0.0) + 1.0
        val by = TechTreeGeometry.nodeY(b, 0.0) + 1.0
        Assertions.assertEquals(id("b"), TechTreeGeometry.nodeAt(layout, setOf(id("a"), id("b")), 0.0, 0.0, bx, by))
        Assertions.assertNull(TechTreeGeometry.nodeAt(layout, setOf(id("a")), 0.0, 0.0, bx, by))
        Assertions.assertNull(TechTreeGeometry.nodeAt(layout, setOf(id("a"), id("b")), 0.0, 0.0, bx + TechTreeGeometry.CELL_WIDTH, by))
    }

    @Test
    fun Geometry_ClampPan_CentresSmallContentAndClampsLarge() {
        Assertions.assertEquals(-50.0, TechTreeGeometry.clampPan(30.0, 100, 200))
        Assertions.assertEquals(0.0, TechTreeGeometry.clampPan(-30.0, 300, 200))
        Assertions.assertEquals(100.0, TechTreeGeometry.clampPan(500.0, 300, 200))
    }
}
