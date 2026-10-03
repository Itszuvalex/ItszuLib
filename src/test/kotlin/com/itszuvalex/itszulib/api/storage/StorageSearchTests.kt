package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.TestableIItemStack
import com.itszuvalex.itszulib.api.adapters.IItemStack
import net.minecraft.resources.Identifier
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private class Subject(
    override val id: Identifier,
    private val name: String,
    private val mod: String = id.namespace,
    private val tags: List<Identifier> = emptyList(),
    private val tooltip: List<String> = emptyList(),
) : SearchSubject {
    var tooltipReads = 0
    override fun name() = name
    override fun modName() = mod
    override fun tags() = tags
    override fun tooltip(): List<String> {
        tooltipReads++
        return tooltip
    }
}

private val IRON = Subject(
    Identifier.fromNamespaceAndPath("minecraft", "iron_ingot"), "Iron Ingot", "Minecraft",
    listOf(Identifier.fromNamespaceAndPath("c", "ingots/iron")), listOf("Iron Ingot", "Smelted"),
)
private val GEAR = Subject(Identifier.fromNamespaceAndPath("gears", "iron_gear"), "Iron Gear", "Gear Works", tooltip = listOf("Iron Gear", "Spins"))

class StorageSearchTest {
    @Test
    fun Parse_PrefixesNegationAndDefaultMode() {
        val search = StorageSearch.parse("  Iron @gears -\$ingots #spin *gear - @ ", SearchMode.NAME)
        Assertions.assertEquals(
            listOf(
                StorageSearch.Term(SearchMode.NAME, "iron", false),
                StorageSearch.Term(SearchMode.MOD, "gears", false),
                StorageSearch.Term(SearchMode.TAG, "ingots", true),
                StorageSearch.Term(SearchMode.TOOLTIP, "spin", false),
                StorageSearch.Term(SearchMode.ID, "gear", false),
            ),
            search.terms,
        )
        Assertions.assertEquals(SearchMode.MOD, StorageSearch.parse("gears", SearchMode.MOD).terms.single().mode, "unprefixed terms use the mode")
        Assertions.assertTrue(StorageSearch.parse("   ").isEmpty)
    }

    @Test
    fun Matches_EachModeAndAllTerms() {
        fun m(q: String, s: Subject) = StorageSearch.parse(q).matches(s)
        Assertions.assertTrue(m("iron", IRON) && m("iron", GEAR))
        Assertions.assertTrue(m("IRON ingot", IRON) && !m("iron ingot", GEAR), "every term must match, any case")
        Assertions.assertTrue(m("@gear", GEAR) && m("@works", GEAR) && !m("@gear", IRON), "mod id or name")
        Assertions.assertTrue(m("\$ingots/iron", IRON) && !m("\$ingots", GEAR), "tags")
        Assertions.assertTrue(m("#smelted", IRON) && !m("#smelted", GEAR), "tooltip")
        Assertions.assertTrue(m("*minecraft:iron", IRON) && !m("*minecraft:", GEAR), "id")
        Assertions.assertTrue(m("iron -gear", IRON) && !m("iron -gear", GEAR), "negation")
    }

    @Test
    fun MatchesItem_ChecksOnlyPerItemTerms() {
        val search = StorageSearch.parse("@minecraft #nothing")
        Assertions.assertTrue(search.matchesItem(IRON), "the tooltip term waits for the stack")
        Assertions.assertFalse(search.matchesItem(GEAR))
        Assertions.assertFalse(search.matchesStack(IRON))
        Assertions.assertTrue(search.needsStack)
        Assertions.assertFalse(StorageSearch.parse("@a \$b *c").needsStack)
    }
}

class StorageEntriesTest {
    private fun id(n: Int) = TestableIItemStack(n).item()

    private fun index(vararg storages: IndexedItemStorage) = ItemStorageIndex().also { i -> storages.forEach(i::add) }

    @Test
    fun Collect_OneEntryPerItemAcrossStorages() {
        val a = IndexedItemStorage(ItemStorageArray(4))
        val b = IndexedItemStorage(ItemStorageArray(4))
        a.setSlot(0, TestableIItemStack(1, 64))
        a.setSlot(2, TestableIItemStack(1, 10))
        b.setSlot(1, TestableIItemStack(1, 6))
        b.setSlot(3, TestableIItemStack(2, 3))
        val entries = StorageEntries.collect(index(a, b), StorageSearch.EMPTY, { error("not asked") }, { error("not asked") })
        Assertions.assertEquals(mapOf(id(1) to 80L, id(2) to 3L), entries.associate { it.stack.item() to it.count })
    }

    @Test
    fun Collect_PerItemTermsSkipSlotsOfOtherItems() {
        val reads = IntArray(1)
        val inner = object : ItemStorageArray(100) {
            override fun get(index: Int): IItemStack {
                reads[0]++
                return super.get(index)
            }
        }
        val storage = IndexedItemStorage(inner)
        for (slot in 0 until 50) storage.setSlot(slot, TestableIItemStack(1, 1))
        storage.setSlot(60, TestableIItemStack(2, 5))
        reads[0] = 0
        val wanted = id(2)
        val entries = StorageEntries.collect(index(storage), StorageSearch.parse("*${wanted.path}"), { Subject(it, it.path) }, { error("no stack terms") })
        Assertions.assertEquals(listOf(5L), entries.map { it.count })
        Assertions.assertTrue(reads[0] <= 2, "read ${reads[0]} slots for one matching slot")
    }

    @Test
    fun Collect_StackTermsCheckEachKind() {
        val storage = IndexedItemStorage(ItemStorageArray(3))
        storage.setSlot(0, TestableIItemStack(1, 2))
        storage.setSlot(1, TestableIItemStack(2, 7))
        val entries = StorageEntries.collect(index(storage), StorageSearch.parse("seven"), { Subject(it, "") }, { s ->
            Subject(s.item(), if (s.stackSize() == 7) "Seven" else "Two")
        })
        Assertions.assertEquals(listOf(7L), entries.map { it.count })
    }

    @Test
    fun Sorted_ByCountNameAndId() {
        val entries = listOf(StorageEntry(TestableIItemStack(1), 5), StorageEntry(TestableIItemStack(2), 50), StorageEntry(TestableIItemStack(3), 5))
        val names = mapOf(id(1) to "b", id(2) to "c", id(3) to "a")
        fun order(sort: TerminalSort) = StorageEntries.sorted(entries, sort) { names.getValue(it.item()) }.map { names.getValue(it.stack.item()) }
        Assertions.assertEquals(listOf("c", "a", "b"), order(TerminalSort.COUNT), "most first, ties by name")
        Assertions.assertEquals(listOf("a", "b", "c"), order(TerminalSort.NAME))
        Assertions.assertEquals(entries.map { it.stack.item().toString() }.sorted(), StorageEntries.sorted(entries, TerminalSort.ID) { "" }.map { it.stack.item().toString() })
    }

    @Test
    fun Pages_ClampAndSplit() {
        val list = (1..10).toList()
        Assertions.assertEquals(1, StorageEntries.pages(0, 4))
        Assertions.assertEquals(3, StorageEntries.pages(10, 4))
        Assertions.assertEquals(listOf(9, 10), StorageEntries.page(list, 2, 4))
        Assertions.assertEquals(listOf(9, 10), StorageEntries.page(list, 7, 4), "past the end shows the last page")
        Assertions.assertEquals(listOf(1, 2, 3, 4), StorageEntries.page(list, -1, 4))
        Assertions.assertEquals(emptyList<Int>(), StorageEntries.page(emptyList<Int>(), 0, 4))
    }

    @Test
    fun FormatCount_FourCharacters() {
        Assertions.assertEquals("999", StorageEntries.formatCount(999))
        Assertions.assertEquals("1k", StorageEntries.formatCount(1000))
        Assertions.assertEquals("1.2k", StorageEntries.formatCount(1250))
        Assertions.assertEquals("12k", StorageEntries.formatCount(12_500))
        Assertions.assertEquals("999k", StorageEntries.formatCount(999_999))
        Assertions.assertEquals("1M", StorageEntries.formatCount(1_000_000))
        Assertions.assertEquals("3.4G", StorageEntries.formatCount(3_400_000_000))
    }
}
