package com.itszuvalex.itszulib.research

import net.minecraft.resources.Identifier

/**
 * Places a tech tree's technologies for drawing: a layered ("Sugiyama") layout, as the 1.7.10 Femtocraft research
 * screen used, read left to right. Pure and deterministic, so it can be tested and cached.
 *
 * 1. Column: 0 for a technology with no prerequisite in the tree, else one more than its furthest prerequisite.
 * 2. A link that skips columns gets a waypoint in each column it crosses, so it can bend around nodes.
 * 3. Order within columns: sweeps that sort each column by the mean row of its neighbours in the previous column
 *    (left to right, then right to left), keeping the order with the fewest crossings.
 * 4. Rows: each node moves towards the mean row of its neighbours, keeping at least one row between nodes in a column.
 *
 * Technologies with a [Technology.position] are drawn there instead (their links lose their waypoints). Links from
 * prerequisites outside the tree are not drawn. A prerequisite cycle (a datapack error, see [TechTree.problems]) is
 * broken arbitrarily.
 */
object TechTreeLayout {
    data class Point(val x: Float, val y: Float)

    /**
     * A link from [from] (a prerequisite) to [to], through [waypoints] (in order, left to right).
     */
    data class Edge(val from: Identifier, val to: Identifier, val waypoints: List<Point>)

    data class Result(val positions: Map<Identifier, Point>, val edges: List<Edge>) {
        val width: Float get() = positions.values.maxOfOrNull { it.x }?.plus(1f) ?: 0f
        val height: Float get() = positions.values.maxOfOrNull { it.y }?.plus(1f) ?: 0f
    }

    private const val SWEEPS = 12
    private const val ALIGN_PASSES = 6

    /** A node of the layered graph: a technology, or a waypoint on a link (id null). */
    private class Node(val id: Identifier?, val column: Int) {
        val parents = ArrayList<Node>()
        val children = ArrayList<Node>()
        var order = 0.0
        var row = 0.0
    }

    @JvmStatic
    fun layout(technologies: Map<Identifier, Technology>): Result = layoutGraph(
        technologies.mapValues { it.value.prerequisites },
        technologies.mapNotNull { (id, t) -> t.position.orElse(null)?.let { id to it } }.toMap(),
    )

    /**
     * The same layout for any graph (e.g. talent trees): [prerequisites] maps every node to the nodes it needs (those
     * not in the map are not drawn), and nodes in [fixed] are drawn at their position.
     */
    @JvmStatic
    @JvmOverloads
    fun layoutGraph(prerequisites: Map<Identifier, List<Identifier>>, fixed: Map<Identifier, Point> = emptyMap()): Result {
        if (prerequisites.isEmpty()) return Result(emptyMap(), emptyList())
        val ids = prerequisites.keys.sorted()
        val parents = ids.associateWith { id -> prerequisites.getValue(id).filter { it in prerequisites && it != id }.distinct().sorted() }
        val column = columns(ids, parents)

        val nodes = ids.associateWith { Node(it, column.getValue(it)) }
        val all = ArrayList<Node>(nodes.values)
        val chains = HashMap<Pair<Identifier, Identifier>, List<Node>>()
        for (id in ids) {
            val child = nodes.getValue(id)
            for (p in parents.getValue(id)) {
                val parent = nodes.getValue(p)
                if (parent.column >= child.column) continue // a broken cycle
                var prev = parent
                val chain = ArrayList<Node>()
                for (c in parent.column + 1 until child.column) {
                    val dummy = Node(null, c)
                    all += dummy
                    chain += dummy
                    link(prev, dummy)
                    prev = dummy
                }
                link(prev, child)
                chains[p to id] = chain
            }
        }

        val layers = all.groupBy { it.column }.toSortedMap().values.map { it.toMutableList() }
        order(layers)
        align(layers)

        val minRow = all.minOf { it.row }
        fun point(n: Node) = Point(n.column.toFloat(), (n.row - minRow).toFloat())
        val positions = LinkedHashMap<Identifier, Point>()
        for (id in ids) positions[id] = fixed[id] ?: point(nodes.getValue(id))
        val edges = ArrayList<Edge>()
        for (id in ids) {
            for (p in parents.getValue(id)) {
                val chain = chains[p to id] ?: continue
                val pinned = id in fixed || p in fixed
                edges += Edge(p, id, if (pinned) emptyList() else chain.map(::point))
            }
        }
        return Result(positions, edges)
    }

    private fun link(parent: Node, child: Node) {
        parent.children += child
        child.parents += parent
    }

    /**
     * Longest path from a root, depth first; a node already on the stack (a cycle) counts as a root for that link.
     */
    private fun columns(ids: List<Identifier>, parents: Map<Identifier, List<Identifier>>): Map<Identifier, Int> {
        val column = HashMap<Identifier, Int>()
        val visiting = HashSet<Identifier>()
        fun visit(id: Identifier): Int {
            column[id]?.let { return it }
            if (!visiting.add(id)) return -1
            val c = (parents.getValue(id).maxOfOrNull(::visit) ?: -1) + 1
            visiting.remove(id)
            column[id] = c
            return c
        }
        ids.forEach(::visit)
        return column
    }

    private fun order(layers: List<MutableList<Node>>) {
        // Initial order: technologies by id, waypoints by their parent's place.
        for (layer in layers) {
            layer.sortWith(compareBy<Node>({ it.parents.firstOrNull()?.order ?: 0.0 }, { it.id?.toString() ?: "" }))
            renumber(layer)
        }
        var best = layers.map { it.toList() }
        var bestCrossings = crossings(layers)
        repeat(SWEEPS) { sweep ->
            val down = sweep % 2 == 0
            val indices = if (down) (1 until layers.size) else (layers.size - 2 downTo 0)
            for (i in indices) {
                val layer = layers[i]
                for (n in layer) {
                    val neighbours = if (down) n.parents else n.children
                    if (neighbours.isNotEmpty()) n.row = neighbours.sumOf { it.order } / neighbours.size else n.row = n.order
                }
                layer.sortWith(compareBy<Node>({ it.row }, { it.order }))
                renumber(layer)
            }
            val c = crossings(layers)
            if (c < bestCrossings) {
                bestCrossings = c
                best = layers.map { it.toList() }
            }
        }
        best.forEachIndexed { i, layer ->
            layers[i].clear()
            layers[i].addAll(layer)
            renumber(layers[i])
        }
    }

    private fun renumber(layer: List<Node>) = layer.forEachIndexed { i, n -> n.order = i.toDouble() }

    private fun crossings(layers: List<List<Node>>): Int {
        var count = 0
        for (layer in layers) {
            val links = layer.flatMap { p -> p.children.map { p.order to it.order } }
            for (a in links.indices) for (b in a + 1 until links.size) {
                val (p1, c1) = links[a]
                val (p2, c2) = links[b]
                if ((p1 < p2 && c1 > c2) || (p1 > p2 && c1 < c2)) count++
            }
        }
        return count
    }

    /**
     * Moves each column's nodes towards their neighbours' mean row, alternating directions. Rows are placed once
     * pushing down from the first node and once pushing up from the last, then averaged: both keep a gap of at least
     * one between neighbours, so their average does too.
     */
    private fun align(layers: List<MutableList<Node>>) {
        for (layer in layers) layer.forEachIndexed { i, n -> n.row = i - (layer.size - 1) / 2.0 }
        repeat(ALIGN_PASSES) { pass ->
            val down = pass % 2 == 0
            val indices = if (down) layers.indices else layers.indices.reversed()
            for (i in indices) {
                val layer = layers[i]
                val desired = layer.map { n ->
                    val neighbours = (if (down) n.parents else n.children).ifEmpty { if (down) n.children else n.parents }
                    if (neighbours.isEmpty()) n.row else neighbours.sumOf { it.row } / neighbours.size
                }
                val forward = DoubleArray(layer.size)
                val backward = DoubleArray(layer.size)
                for (k in layer.indices) forward[k] = if (k == 0) desired[k] else maxOf(desired[k], forward[k - 1] + 1)
                for (k in layer.indices.reversed()) backward[k] = if (k == layer.size - 1) desired[k] else minOf(desired[k], backward[k + 1] - 1)
                for (k in layer.indices) layer[k].row = (forward[k] + backward[k]) / 2
            }
        }
    }
}
