package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.adapters.IItemStack
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.resources.Identifier

/**
 * What a search term looks at. A term picks its mode with a prefix (`@` mod, `#` tooltip, `$` tag, `*` id, as JEI and
 * AE2 do); a term without one uses the terminal's chosen mode. [perItem] modes depend on the item alone, so they are
 * checked once per item id before any slot is read; the others depend on the stack (a renamed item, an enchantment).
 */
enum class SearchMode(val prefix: Char?, val perItem: Boolean) {
    NAME(null, false),
    MOD('@', true),
    TOOLTIP('#', false),
    TAG('$', true),
    ID('*', true),
    ;

    companion object {
        @JvmStatic
        fun ofPrefix(c: Char): SearchMode? = entries.firstOrNull { it.prefix == c }
    }
}

/**
 * What a search matches against: an item, or one stack of it. Everything but [id] is asked for only when a term
 * needs it, so subjects can compute names and tooltips lazily.
 */
interface SearchSubject {
    val id: Identifier

    /** The display name (of the stack, when it is one). */
    fun name(): String

    /** The mod's display name; its id is [id]'s namespace. */
    fun modName(): String = id.namespace

    /** The ids of the tags the item is in. */
    fun tags(): Collection<Identifier> = emptyList()

    /** The stack's tooltip lines (empty for an item). */
    fun tooltip(): List<String> = emptyList()
}

/**
 * A parsed search: space-separated terms, all of which must match (a term starting with `-` must not). Case is
 * ignored. Built with [parse].
 */
class StorageSearch(val terms: List<Term>) {
    data class Term(val mode: SearchMode, val text: String, val negated: Boolean)

    val isEmpty: Boolean get() = terms.isEmpty()

    /** Whether some term needs the stack (so each stack of a matching item is checked with [matchesStack]). */
    val needsStack: Boolean get() = terms.any { !it.mode.perItem }

    /** Whether [subject] passes the per-item terms (the others are not checked). */
    fun matchesItem(subject: SearchSubject): Boolean = terms.all { !it.mode.perItem || matches(it, subject) }

    /** Whether [subject] passes the per-stack terms (the others are not checked). */
    fun matchesStack(subject: SearchSubject): Boolean = terms.all { it.mode.perItem || matches(it, subject) }

    /** Whether [subject] passes every term. */
    fun matches(subject: SearchSubject): Boolean = terms.all { matches(it, subject) }

    private fun matches(term: Term, subject: SearchSubject): Boolean = matchesTerm(term, subject) != term.negated

    private fun matchesTerm(term: Term, subject: SearchSubject): Boolean {
        val t = term.text
        return when (term.mode) {
            SearchMode.NAME -> subject.name().lowercase().contains(t)
            SearchMode.MOD -> subject.id.namespace.lowercase().contains(t) || subject.modName().lowercase().contains(t)
            SearchMode.TOOLTIP -> subject.tooltip().any { it.lowercase().contains(t) }
            SearchMode.TAG -> subject.tags().any { it.toString().lowercase().contains(t) }
            SearchMode.ID -> subject.id.toString().lowercase().contains(t)
        }
    }

    companion object {
        @JvmField
        val EMPTY = StorageSearch(emptyList())

        /**
         * Parses [query]: terms split on spaces, each with an optional `-` (negate) and a mode prefix; terms without a
         * prefix use [mode]. A bare prefix or `-` is ignored.
         */
        @JvmStatic
        @JvmOverloads
        fun parse(query: String, mode: SearchMode = SearchMode.NAME): StorageSearch {
            val terms = query.trim().split(' ').mapNotNull { raw ->
                var s = raw.lowercase()
                val negated = s.startsWith('-')
                if (negated) s = s.substring(1)
                val prefixed = s.firstOrNull()?.let(SearchMode::ofPrefix)
                if (prefixed != null) s = s.substring(1)
                if (s.isEmpty()) null else Term(prefixed ?: mode, s, negated)
            }
            return StorageSearch(terms)
        }
    }
}

/** How a terminal orders its entries. */
enum class TerminalSort { COUNT, NAME, ID }

/**
 * One kind of stack held across an [ItemStorageIndex] (one item with one set of components) and how many there are.
 * [stack] is one of them, as found in a slot (its size is not the count).
 */
data class StorageEntry(val stack: IItemStack, val count: Long)

/**
 * Collects the entries of an [ItemStorageIndex] that match a [StorageSearch], through the index: items that fail the
 * per-item terms are dropped from the index's item list without reading a slot, and only the slots of the remaining
 * items are read (to tell stacks apart and count them).
 */
object StorageEntries {
    /**
     * @param itemSubject The item-level subject for an id (names, mod names, tags).
     * @param stackSubject The stack-level subject (display name, tooltip); only asked when the search needs it.
     */
    @JvmStatic
    fun collect(
        index: ItemStorageIndex,
        search: StorageSearch,
        itemSubject: (Identifier) -> SearchSubject,
        stackSubject: (IItemStack) -> SearchSubject,
    ): List<StorageEntry> {
        val result = ArrayList<StorageEntry>()
        for (id in index.items().toList()) {
            if (!search.isEmpty && !search.matchesItem(itemSubject(id))) continue
            val kinds = LinkedHashMap<DataComponentPatch, StorageEntry>()
            for ((storage, slot) in index.find(id)) {
                val stack = storage.get(slot)
                if (stack.isEmpty()) continue
                val key = stack.components()
                val prior = kinds[key]
                kinds[key] = if (prior == null) StorageEntry(stack.copy(), stack.stackSize().toLong()) else prior.copy(count = prior.count + stack.stackSize())
            }
            for (entry in kinds.values) {
                if (search.needsStack && !search.matchesStack(stackSubject(entry.stack))) continue
                result += entry
            }
        }
        return result
    }

    /** [entries] ordered by [sort]: most first, by name, or by id (ties by name). */
    @JvmStatic
    fun sorted(entries: List<StorageEntry>, sort: TerminalSort, nameOf: (IItemStack) -> String): List<StorageEntry> =
        sorted(entries, sort, { it.count }, { nameOf(it.stack) }, { it.stack.item().toString() })

    /** [entries] of any kind ordered by [sort], reading their [count], [name] and [id]. */
    @JvmStatic
    fun <T> sorted(entries: List<T>, sort: TerminalSort, count: (T) -> Long, name: (T) -> String, id: (T) -> String): List<T> {
        val names = HashMap<T, String>()
        fun n(e: T) = names.getOrPut(e) { name(e).lowercase() }
        val comparator: Comparator<T> = when (sort) {
            TerminalSort.COUNT -> compareByDescending<T> { count(it) }.thenBy { n(it) }
            TerminalSort.NAME -> compareBy<T> { n(it) }.thenByDescending { count(it) }
            TerminalSort.ID -> compareBy<T> { id(it) }.thenBy { n(it) }
        }
        return entries.sortedWith(comparator)
    }

    /** The number of pages of [size] for [count] entries (at least one). */
    @JvmStatic
    fun pages(count: Int, size: Int): Int = maxOf(1, (count + size - 1) / size)

    /** Page [page] (clamped to the pages there are) of [size] of [entries]. */
    @JvmStatic
    fun <T> page(entries: List<T>, page: Int, size: Int): List<T> {
        val p = page.coerceIn(0, pages(entries.size, size) - 1)
        return entries.subList(minOf(p * size, entries.size), minOf((p + 1) * size, entries.size))
    }

    /** A count in at most four characters, rounded down (never more than is held): 999, 1.2k, 12k, 999k, 1.2M, ... */
    @JvmStatic
    fun formatCount(count: Long): String {
        if (count < 1000) return count.toString()
        val units = "kMGTPE"
        var value = count.toDouble()
        var unit = -1
        while (value >= 1000 && unit < units.length - 1) {
            value /= 1000
            unit++
        }
        val text = if (value < 10) {
            val tenths = kotlin.math.floor(value * 10).toLong()
            if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
        } else {
            value.toLong().toString()
        }
        return text + units[unit]
    }
}
